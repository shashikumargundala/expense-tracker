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
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import com.shashi.expensetracker.domain.inbox.Dismissal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AD-4's push of `UpsertExpense` and `DeleteExpense` with the base check: every row of story 3.4's
 * matrix, through a whole [SyncEngine] run against [FakeExpenseSheetGateway] and a real in-memory
 * mirror. Entries are inserted directly with their base, as edits and deletes will record them.
 */
@RunWith(AndroidJUnit4::class)
class ExpensePushTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomExpenseRepository
  private val sheet = FakeExpenseSheetGateway()
  private val createdAt = LocalDateTime.of(2026, 11, 13, 9, 30)

  /** A Sheet row as last pulled, and so the base of an edit or delete. */
  private val pulled = sampleExpense(id = "a").copy(source = ExpenseSource.User)

  /** The pending in-app edit of [pulled]. */
  private val edited = pulled.copy(amountPaise = 99_900, description = "Lunch")

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    repository = RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock)
  }

  @After fun tearDown() = db.close()

  private fun engine(gateway: ExpenseSheetGateway = sheet) =
    SyncEngine(gateway, db.expenseDao(), db.categoryDao(), db.outboxDao(), db.mirrorRefresh(), testClock)

  private suspend fun sync() = engine().sync("sheet-1")

  private suspend fun entry(type: OutboxType, key: String, base: Expense?) {
    db.outboxDao().insert(OutboxEntity(type = type, rowKey = key, base = base?.let(ExpenseBase::encode), createdAt = createdAt))
  }

  /** A pending edit: the mirror holds [now] and the entry holds [base]. */
  private suspend fun edit(now: Expense, base: Expense? = pulled) {
    db.expenseDao().upsert(now.toEntity())
    entry(OutboxType.UpsertExpense, now.id, base)
  }

  /** A pending delete: the mirror row is gone and the entry holds [base]. */
  private suspend fun delete(base: Expense, key: String = base.id) {
    db.expenseDao().deleteByIds(listOf(key))
    entry(OutboxType.DeleteExpense, key, base)
  }

  private suspend fun mirror(): List<Expense> = db.expenseDao().getAll().map { it.toDomain() }.sortedBy { it.id }

  private suspend fun outboxKeys() = db.outboxDao().getAll().map { it.rowKey }

  @Test
  fun `an upsert whose row equals its base updates the row in place and leaves`() = runTest {
    sheet.expenses += pulled
    edit(edited)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(edited to 2), sheet.writes)
    assertEquals(listOf(edited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(edited), mirror())
  }

  @Test
  fun `an upsert whose row changed in the Sheet writes nothing, and the Sheet's value wins`() = runTest {
    val handEdited = pulled.copy(amountPaise = 5_000)
    sheet.expenses += handEdited
    edit(edited)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf(handEdited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(handEdited), mirror())
  }

  @Test
  fun `an upsert whose row was deleted in the Sheet writes nothing and the mirror row goes`() = runTest {
    edit(edited)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.writes.isEmpty())
    assertTrue(sheet.expenses.isEmpty())
    assertTrue(outboxKeys().isEmpty())
    assertTrue(mirror().isEmpty())
  }

  @Test
  fun `a dropped upsert whose SMS-sourced row was deleted in the Sheet sends its SMS ID to Dismissed`() = runTest {
    val fromSms = pulled.copy(smsId = "h:abc")
    edit(edited.copy(smsId = "h:abc"), base = fromSms)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    val dismissed = Dismissal("h:abc", createdAt)
    assertEquals(listOf(dismissed), sheet.appendedDismissals)
    assertEquals(listOf(dismissed), db.dismissalDao().getAll().map { it.toDomain() })
    assertTrue(outboxKeys().isEmpty())
    assertTrue(mirror().isEmpty())
  }

  @Test
  fun `a create with no row is appended with Source App`() = runTest {
    val created = sampleExpense(id = "new")
    repository.add(created)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(created to null), sheet.writes)
    assertEquals(ExpenseSource.App, sheet.expenses.single().source)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a retried create whose row already landed writes nothing and leaves one row`() = runTest {
    val created = sampleExpense(id = "new")
    sheet.expenses += created
    repository.add(created)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf(created), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a create whose ID is a different row in the Sheet writes nothing and takes the Sheet's row`() = runTest {
    val created = sampleExpense(id = "new")
    val other = created.copy(amountPaise = 1, source = ExpenseSource.User)
    sheet.expenses += other
    repository.add(created)

    sync()

    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf(other), mirror())
  }

  @Test
  fun `a delete whose row equals its base deletes the row and leaves`() = runTest {
    val other = sampleExpense(id = "b")
    sheet.expenses += other
    sheet.expenses += pulled
    delete(pulled)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(listOf(3)), sheet.deletes)
    assertEquals(listOf(other), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(other), mirror())
  }

  @Test
  fun `a delete whose row changed in the Sheet keeps the row, and the mirror gets it back`() = runTest {
    val handEdited = pulled.copy(description = "Tea")
    sheet.expenses += handEdited
    delete(pulled)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.deletes.isEmpty())
    assertEquals(listOf(handEdited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(handEdited), mirror())
  }

  @Test
  fun `a delete whose row is already gone does nothing and leaves`() = runTest {
    delete(pulled)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.deletes.isEmpty())
    assertTrue(outboxKeys().isEmpty())
    assertTrue(mirror().isEmpty())
  }

  @Test
  fun `a base pulled from a blank-Source row matches it, and the update leaves Source blank`() = runTest {
    sheet.expenseRows += ExpenseRowRead("a", pulled, sourceBlank = true)
    edit(edited)
    // The Sheet still has the blank Source when the push reads it.
    val noSourceRepair =
      object : ExpenseSheetGateway by sheet {
        override suspend fun writeExpenseRepairs(sheetId: String, fixes: List<ExpenseRowFix>) = RowWriteResult.Written(null)
      }

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), engine(noSourceRepair).sync("sheet-1"))

    assertEquals(listOf(edited to 2), sheet.writes)
    assertEquals(ExpenseRowRead("a", edited, sourceBlank = true), sheet.expenseRows.single())
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a duplicated row repaired to one gets the change, and no second row appears`() = runTest {
    sheet.expenses += pulled
    sheet.expenses += pulled
    edit(edited)

    sync()

    assertEquals(listOf(listOf(3)), sheet.deletes)
    assertEquals(listOf(edited to 2), sheet.writes)
    assertEquals(listOf(edited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `two upserts for a key are one change checked against the first base`() = runTest {
    sheet.expenses += pulled
    val first = pulled.copy(amountPaise = 1_000)
    edit(first)
    // A later edit of a pending key keeps the first base, but even another base is never checked.
    edit(edited, base = first)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(edited to 2), sheet.writes)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `when the first base differs, every entry for the key is dropped`() = runTest {
    sheet.expenses += pulled
    edit(edited, base = pulled.copy(amountPaise = 1))
    edit(edited.copy(description = "Dinner"), base = pulled)

    sync()

    assertTrue(sheet.writes.isEmpty())
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(pulled), mirror())
  }

  @Test
  fun `an upsert then a delete for a key is one delete`() = runTest {
    sheet.expenses += pulled
    edit(edited)
    delete(base = edited, key = "a")

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf(listOf(2)), sheet.deletes)
    assertTrue(sheet.expenses.isEmpty())
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a create then a delete before any sync writes nothing`() = runTest {
    repository.add(sampleExpense(id = "new"))
    delete(base = sampleExpense(id = "new"))

    sync()

    assertTrue(sheet.writes.isEmpty())
    assertTrue(sheet.deletes.isEmpty())
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `an upsert and a delete for different keys update one row and delete the other`() = runTest {
    val gone = sampleExpense(id = "b").copy(description = "Bus")
    sheet.expenses += gone
    sheet.expenses += pulled
    delete(gone)
    edit(edited)

    assertEquals(SyncResult.Synced(pushed = 2, skipped = 0), sync())

    // The write goes first, then the delete, at the rows of the same read.
    assertEquals(listOf(edited to 3), sheet.writes)
    assertEquals(listOf(listOf(2)), sheet.deletes)
    assertEquals(listOf(edited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(edited), mirror())
  }

  @Test
  fun `several deletes go in one request`() = runTest {
    val b = sampleExpense(id = "b")
    val c = sampleExpense(id = "c")
    sheet.expenses += b
    sheet.expenses += pulled
    sheet.expenses += c
    delete(b)
    delete(c)

    sync()

    assertEquals(listOf(listOf(2, 4)), sheet.deletes)
    assertEquals(listOf(pulled), sheet.expenses)
  }

  @Test
  fun `a failed or refused write keeps the key's entries, skips the refresh and the rerun applies`() = runTest {
    sheet.expenses += pulled
    edit(edited)
    val failures =
      listOf(
        RowWriteResult.Failure(ExpenseSheetFailure.Network) to SyncFailure.Network,
        RowWriteResult.NeedsConsent(object : ConsentRequest {}) to SyncFailure.NeedsConsent,
      )

    failures.forEach { (result, failure) ->
      sheet.writeResult = result
      assertEquals(SyncResult.Failed(failure), sync())
    }
    assertEquals(listOf("a"), outboxKeys())
    assertEquals(listOf(edited), mirror())
    assertEquals(listOf(pulled), sheet.expenses)

    sheet.writeResult = null
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())
    assertEquals(listOf(edited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a failed delete keeps only the delete's entries, and the rerun deletes`() = runTest {
    val gone = sampleExpense(id = "b")
    sheet.expenses += pulled
    sheet.expenses += gone
    edit(edited)
    delete(gone)
    sheet.deleteResult = RowWriteResult.NeedsConsent(object : ConsentRequest {})

    assertEquals(SyncResult.Failed(SyncFailure.NeedsConsent), sync())

    // The confirmed write already left; the delete stays for the next run.
    assertEquals(listOf("b"), outboxKeys())
    assertEquals(listOf(edited, gone), sheet.expenses)
    assertEquals(listOf(edited), mirror())

    sheet.deleteResult = null
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())
    assertEquals(listOf(edited), sheet.expenses)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a key whose only row is invalid is kept and never written or deleted`() = runTest {
    sheet.expenseRows += ExpenseRowRead("a", null)
    delete(pulled)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 1), sync())

    assertTrue(sheet.deletes.isEmpty())
    assertEquals(listOf("a"), outboxKeys())
  }

  @Test
  fun `an unreadable base counts as changed in the Sheet`() = runTest {
    sheet.expenses += pulled
    db.expenseDao().upsert(edited.toEntity())
    db.outboxDao().insert(OutboxEntity(type = OutboxType.UpsertExpense, rowKey = "a", base = "{", createdAt = createdAt))

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.writes.isEmpty())
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(pulled), mirror())
  }
}
