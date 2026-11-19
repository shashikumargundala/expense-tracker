package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.expense.RoomExpenseRepository
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.inbox.toDomain
import com.shashi.expensetracker.data.mirrorRefresh
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import com.shashi.expensetracker.domain.inbox.Dismissal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A sync run's AD-5 ID repair and AD-13 vanished-SMS step, against [FakeExpenseSheetGateway] and a
 * real in-memory mirror: every row of story 3.3's matrix.
 */
@RunWith(AndroidJUnit4::class)
class SyncEngineRepairTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomExpenseRepository
  private lateinit var engine: SyncEngine
  private val sheet = FakeExpenseSheetGateway()

  /** `now` from [testClock]. */
  private val now = LocalDateTime.of(2026, 11, 13, 9, 30)

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    repository = RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock)
    engine = SyncEngine(sheet, db.expenseDao(), db.outboxDao(), db.mirrorRefresh(), testClock)
  }

  @After fun tearDown() = db.close()

  private suspend fun mirror(): List<Expense> = db.expenseDao().getAll().map { it.toDomain() }.sortedBy { it.id }

  private suspend fun mirrorDismissals() = db.dismissalDao().getAll().map { it.toDomain() }

  /** A hand-typed row: no ID and, by default, no Source. */
  private fun handTyped(expense: Expense, sourceBlank: Boolean = true) =
    ExpenseRowRead(null, expense.copy(id = "", source = ExpenseSource.User), sourceBlank)

  private fun invalid(id: String) = ExpenseRowRead(id, null, sourceBlank = false)

  /** Fills the tab so the next row added is row [row]. */
  private fun fillTo(row: Int) {
    while (sheet.expenseRows.size + 2 < row) sheet.expenses += sampleExpense(id = "fill-${sheet.expenseRows.size + 2}")
  }

  @Test
  fun `a hand-typed row with no ID and no Source gets a UUID and User, and is mirrored`() = runTest {
    sheet.expenseRows += handTyped(sampleExpense(amountPaise = 5_000))

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), engine.sync("sheet-1"))

    val row = sheet.expenseRows.single()
    val id = checkNotNull(row.id)
    assertTrue(id, id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    assertFalse(row.sourceBlank)
    assertEquals(listOf(listOf(ExpenseRowFix(2, id = id, sourceUser = true))), sheet.repairWrites)
    assertEquals(2, sheet.readExpensesCalls)
    assertEquals(listOf(sampleExpense(id = id, amountPaise = 5_000).copy(source = ExpenseSource.User)), mirror())
  }

  @Test
  fun `a keyed row with a blank Source gets User written, with no re-read`() = runTest {
    val expense = sampleExpense(id = "a").copy(source = ExpenseSource.User)
    sheet.expenseRows += ExpenseRowRead("a", expense, sourceBlank = true)

    engine.sync("sheet-1")

    assertEquals(listOf(listOf(ExpenseRowFix(2, sourceUser = true))), sheet.repairWrites)
    assertFalse(sheet.expenseRows.single().sourceBlank)
    assertEquals(1, sheet.readExpensesCalls)
    assertEquals(listOf(expense), mirror())
  }

  @Test
  fun `a copied row with identical values is deleted, leaving one in the Sheet and the mirror`() = runTest {
    val x = sampleExpense(id = "x")
    fillTo(5)
    sheet.expenses += x
    fillTo(9)
    sheet.expenses += x.copy(category = "food")

    engine.sync("sheet-1")

    assertEquals(listOf(listOf(9)), sheet.deletes)
    assertTrue(sheet.repairWrites.isEmpty())
    assertEquals(1, sheet.expenses.count { it.id == "x" })
    assertEquals(5, sheet.expenses.indexOfFirst { it.id == "x" } + 2)
    assertEquals(listOf(x), mirror().filter { it.id == "x" })
    assertEquals(7, mirror().size)
  }

  @Test
  fun `a differing duplicate gets a new UUID and a cleared SMS ID, and both are mirrored`() = runTest {
    val x = sampleExpense(id = "x")
    fillTo(5)
    sheet.expenses += x
    fillTo(9)
    sheet.expenses += x.copy(amountPaise = 999, smsId = "s")

    engine.sync("sheet-1")

    val fix = sheet.repairWrites.single().single()
    val newId = checkNotNull(fix.id)
    assertEquals(ExpenseRowFix(9, id = newId, clearSmsId = true), fix)
    assertTrue(sheet.deletes.isEmpty())
    assertEquals(x.copy(id = newId, amountPaise = 999), sheet.expenses[7])
    assertEquals(x, db.expenseDao().get("x")?.toDomain())
    assertEquals(x.copy(id = newId, amountPaise = 999), db.expenseDao().get(newId)?.toDomain())
  }

  @Test
  fun `several duplicates go in one delete and the rows in between survive`() = runTest {
    val x = sampleExpense(id = "x")
    sheet.expenses += x
    fillTo(4)
    sheet.expenses += x
    fillTo(7)
    sheet.expenses += x
    fillTo(12)
    sheet.expenses += x

    engine.sync("sheet-1")

    assertEquals(1, sheet.deletes.size)
    assertEquals(setOf(4, 7, 12), sheet.deletes.single().toSet())
    val survivors = listOf("x", "fill-3", "fill-5", "fill-6", "fill-8", "fill-9", "fill-10", "fill-11")
    assertEquals(survivors, sheet.expenses.map { it.id })
    assertEquals(survivors.sorted(), mirror().map { it.id })
  }

  @Test
  fun `an SMS-sourced Expense gone from the Sheet has its SMS ID appended to Dismissed and mirrored`() = runTest {
    db.expenseDao().upsert(sampleExpense(id = "e1").copy(smsId = "s1").toEntity())
    db.expenseDao().upsert(sampleExpense(id = "e4").toEntity())

    engine.sync("sheet-1")

    assertEquals(listOf(Dismissal("s1", now)), sheet.appendedDismissals)
    assertEquals(listOf(Dismissal("s1", now)), mirrorDismissals())
    assertTrue(mirror().isEmpty())
  }

  @Test
  fun `a gone SMS ID that Dismissed already has is not appended again`() = runTest {
    val earlier = LocalDateTime.of(2026, 11, 1, 8, 0)
    db.expenseDao().upsert(sampleExpense(id = "e1").copy(smsId = "s1").toEntity())
    sheet.dismissals += Dismissal("s1", earlier)

    engine.sync("sheet-1")

    assertTrue(sheet.appendedDismissals.isEmpty())
    assertEquals(listOf(Dismissal("s1", earlier)), mirrorDismissals())
  }

  @Test
  fun `an invalid row is not gone, adds no Dismissed row, keeps its mirror row and is counted`() = runTest {
    val e2 = sampleExpense(id = "e2").copy(smsId = "s2")
    db.expenseDao().upsert(e2.toEntity())
    sheet.expenseRows += invalid("e2")
    sheet.expenseRows += ExpenseRowRead(null, null, sourceBlank = true)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 2), engine.sync("sheet-1"))

    assertTrue(sheet.appendedDismissals.isEmpty())
    assertTrue(sheet.repairWrites.isEmpty())
    assertTrue(sheet.deletes.isEmpty())
    assertEquals(listOf(e2), mirror())
  }

  @Test
  fun `a pending Expense whose ID is only an invalid row is not written and stays pending`() = runTest {
    val e5 = sampleExpense(id = "e5")
    repository.add(e5)
    sheet.expenseRows += invalid("e5")

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 1), engine.sync("sheet-1"))

    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf(invalid("e5")), sheet.expenseRows)
    assertEquals(listOf("e5"), db.outboxDao().getAll().map { it.rowKey })
    assertEquals(listOf(e5), mirror())
  }

  @Test
  fun `a pending Expense missing from the Sheet is not gone and is pushed`() = runTest {
    val e3 = sampleExpense(id = "e3").copy(smsId = "s3")
    repository.add(e3)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), engine.sync("sheet-1"))

    assertTrue(sheet.appendedDismissals.isEmpty())
    assertEquals(listOf(e3), sheet.expenses)
  }

  @Test
  fun `a failed or refused repair write or delete stops before any push, and a failed Dismissed append before the refresh`() = runTest {
    repository.add(sampleExpense(id = "kept"))
    db.expenseDao().upsert(sampleExpense(id = "gone").copy(smsId = "s1").toEntity())
    val x = sampleExpense(id = "x")
    sheet.expenseRows += handTyped(sampleExpense(amountPaise = 5_000))
    sheet.expenses += x
    sheet.expenses += x
    val failures =
      listOf(
        RowWriteResult.Failure(ExpenseSheetFailure.Network) to SyncFailure.Network,
        RowWriteResult.NeedsConsent(object : ConsentRequest {}) to SyncFailure.NeedsConsent,
        RowWriteResult.SheetMissing to SyncFailure.SheetMissing,
      )
    val scripts =
      listOf<FakeExpenseSheetGateway.(RowWriteResult?) -> Unit>(
        { repairWriteResult = it },
        { deleteResult = it },
        { appendDismissalsResult = it },
      )

    // Each step runs only after the earlier ones succeed, so later scripts see a partly repaired Sheet.
    scripts.forEach { script ->
      failures.forEach { (result, failure) ->
        sheet.script(result)
        assertEquals(SyncResult.Failed(failure), engine.sync("sheet-1"))
        sheet.script(null)
      }
    }
    // Three failed deletes, then the one that let the Dismissed append run.
    assertEquals(4, sheet.deletes.size)

    // The Dismissed append runs after the push, so only its first failed run pushed the entry.
    assertEquals(listOf("kept"), sheet.writes.map { it.first.id })
    assertTrue(db.outboxDao().getAll().isEmpty())
    assertEquals(listOf("gone", "kept"), mirror().map { it.id })
  }

  @Test
  fun `a rerun after a failed delete recomputes from a fresh read and finishes the repair`() = runTest {
    val x = sampleExpense(id = "x")
    sheet.expenseRows += handTyped(sampleExpense(amountPaise = 5_000))
    sheet.expenses += x
    sheet.expenses += x
    sheet.deleteResult = RowWriteResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(SyncResult.Failed(SyncFailure.Network), engine.sync("sheet-1"))
    val newId = checkNotNull(sheet.expenseRows.first().id)
    assertTrue(mirror().isEmpty())

    sheet.deleteResult = null
    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), engine.sync("sheet-1"))

    // The blank ID already has its UUID, so the rerun only deletes.
    assertEquals(1, sheet.repairWrites.size)
    assertEquals(listOf(listOf(4), listOf(4)), sheet.deletes)
    assertEquals(listOf(newId, "x"), sheet.expenses.map { it.id })
    assertEquals(listOf(newId, "x").sorted(), mirror().map { it.id })
    assertNotNull(db.expenseDao().get(newId))
  }
}
