package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.category.CategoryEntity
import com.shashi.expensetracker.data.expense.RoomExpenseRepository
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The tracer sync against [FakeExpenseSheetGateway] and a real in-memory mirror (the I/O matrix). */
@RunWith(AndroidJUnit4::class)
class SyncEngineTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomExpenseRepository
  private lateinit var engine: SyncEngine
  private val sheet = FakeExpenseSheetGateway()

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    repository = RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock)
    engine = newEngine()
  }

  private fun newEngine(gateway: com.shashi.expensetracker.domain.account.ExpenseSheetGateway = sheet) =
    SyncEngine(gateway, db.expenseDao(), db.outboxDao(), MirrorRefresh(db, db.expenseDao(), db.categoryDao(), db.outboxDao()))

  @After fun tearDown() = db.close()

  private suspend fun pending() = repository.observe().first().associate { it.expense.id to it.pending }

  @Test
  fun `a new Expense is appended with Source App, leaves the outbox and stops pending`() = runTest {
    val expense = sampleExpense(id = "new")
    repository.add(expense)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), engine.sync("sheet-1"))

    assertEquals(listOf(expense to null), sheet.writes)
    assertEquals(listOf(expense), sheet.expenses)
    assertEquals(ExpenseSource.App, sheet.expenses.single().source)
    assertTrue(db.outboxDao().getAll().isEmpty())
    assertEquals(mapOf("new" to false), pending())
  }

  @Test
  fun `a retry whose ID is already a row updates that row and never duplicates`() = runTest {
    val other = sampleExpense(id = "other").copy(source = ExpenseSource.User)
    val expense = sampleExpense(id = "retry")
    sheet.expenses += other
    sheet.expenses += expense.copy(amountPaise = 1)
    repository.add(expense)

    engine.sync("sheet-1")

    assertEquals(listOf(expense to 3), sheet.writes)
    assertEquals(listOf(other, expense), sheet.expenses)
    assertTrue(db.outboxDao().getAll().isEmpty())
  }

  @Test
  fun `rows are found by ID in the fresh read, not by a stored index`() = runTest {
    val expense = sampleExpense(id = "moved")
    repository.add(expense)
    // The Sheet was sorted: the row now sits below two others.
    sheet.expenses += sampleExpense(id = "x")
    sheet.expenses += sampleExpense(id = "y")
    sheet.expenses += expense

    engine.sync("sheet-1")

    assertEquals(4, sheet.writes.single().second)
  }

  @Test
  fun `offline, access, consent and missing-sheet failures keep the entry and the mirror`() = runTest {
    val expense = sampleExpense(id = "kept")
    repository.add(expense)
    db.categoryDao().upsertAll(listOf(CategoryEntity("Food", "#F59E0B", "restaurant")))
    sheet.categories += Category("Bills", "#3B82F6", "receipt_long")
    val failures =
      listOf(
        TabReadResult.Failure(ExpenseSheetFailure.Network) to SyncFailure.Network,
        TabReadResult.Failure(ExpenseSheetFailure.Access) to SyncFailure.Access,
        TabReadResult.NeedsConsent(object : ConsentRequest {}) to SyncFailure.NeedsConsent,
        TabReadResult.SheetMissing to SyncFailure.SheetMissing,
      )

    failures.forEach { (read, failure) ->
      sheet.readExpensesResult = read
      assertEquals(SyncResult.Failed(failure), engine.sync("sheet-1"))
    }

    assertEquals(1, db.outboxDao().getAll().size)
    assertEquals(mapOf("kept" to true), pending())
    assertEquals(listOf("Food"), db.categoryDao().getAll().map { it.name })
    assertTrue(sheet.writes.isEmpty())
  }

  @Test
  fun `a trashed, missing or unreachable Sheet keeps the entry and writes nothing`() = runTest {
    repository.add(sampleExpense(id = "kept"))
    val checks =
      listOf(
        com.shashi.expensetracker.domain.account.ExpenseSheetResult.SheetMissing to SyncFailure.SheetMissing,
        com.shashi.expensetracker.domain.account.ExpenseSheetResult.NeedsConsent(object : ConsentRequest {}) to
          SyncFailure.NeedsConsent,
        com.shashi.expensetracker.domain.account.ExpenseSheetResult.Failure(ExpenseSheetFailure.Network) to
          SyncFailure.Network,
      )

    checks.forEach { (check, failure) ->
      sheet.checkResult = check
      assertEquals(SyncResult.Failed(failure), engine.sync("sheet-1"))
    }

    assertTrue(sheet.writes.isEmpty())
    assertEquals(0, sheet.readExpensesCalls)
    assertEquals(1, db.outboxDao().getAll().size)
    assertEquals(mapOf("kept" to true), pending())
  }

  @Test
  fun `a failed write keeps the entry, skips the refresh and the next run retries`() = runTest {
    val expense = sampleExpense(id = "flaky")
    repository.add(expense)
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.writeResult = RowWriteResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(SyncResult.Failed(SyncFailure.Network), engine.sync("sheet-1"))
    assertEquals(1, db.outboxDao().getAll().size)
    assertTrue(db.categoryDao().getAll().isEmpty())

    sheet.writeResult = null
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), engine.sync("sheet-1"))
    assertEquals(listOf(expense), sheet.expenses)
    assertTrue(db.outboxDao().getAll().isEmpty())
  }

  @Test
  fun `a write needing consent or hitting a missing sheet stops the run`() = runTest {
    repository.add(sampleExpense(id = "a"))
    sheet.writeResult = RowWriteResult.NeedsConsent(object : ConsentRequest {})
    assertEquals(SyncResult.Failed(SyncFailure.NeedsConsent), engine.sync("sheet-1"))

    sheet.writeResult = RowWriteResult.SheetMissing
    assertEquals(SyncResult.Failed(SyncFailure.SheetMissing), engine.sync("sheet-1"))

    assertEquals(1, db.outboxDao().getAll().size)
  }

  @Test
  fun `the refresh upserts pulled Expenses and Categories and deletes missing rows unless pending`() = runTest {
    val handTyped = sampleExpense(id = "hand").copy(source = ExpenseSource.User, description = "Auto")
    val edited = sampleExpense(id = "edited")
    val gone = sampleExpense(id = "gone")
    db.expenseDao().upsertAll(listOf(edited.copy(amountPaise = 1).toEntity(), gone.toEntity()))
    db.categoryDao().upsertAll(listOf(CategoryEntity("food", "#000000", ""), CategoryEntity("Old", "", "")))
    sheet.expenses += handTyped
    sheet.expenses += edited
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.categories += Category("Fun", "#A855F7", "celebration")
    // Pushed during this run, so the re-read before the refresh has it.
    val unsynced = sampleExpense(id = "unsynced")
    repository.add(unsynced)
    db.outboxDao().insert(OutboxEntity(type = OutboxType.UpsertCategory, rowKey = "Old", base = null, createdAt = LocalDateTime.MIN))

    // UpsertCategory isn't applied by this tracer, so only the Expense is pushed.
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), engine.sync("sheet-1"))

    assertEquals(
      setOf(handTyped, edited, unsynced),
      db.expenseDao().getAll().map { it.toDomain() }.toSet(),
    )
    assertEquals(
      setOf(CategoryEntity("Food", "#F59E0B", "restaurant"), CategoryEntity("Fun", "#A855F7", "celebration"), CategoryEntity("Old", "", "")),
      db.categoryDao().getAll().toSet(),
    )
    assertEquals(listOf(OutboxType.UpsertCategory), db.outboxDao().getAll().map { it.type })
  }

  @Test
  fun `the refresh leaves a row with a pending entry as it is locally`() = runTest {
    // DeleteExpense isn't applied by this tracer, so its entry stays pending through the refresh.
    val local = sampleExpense(id = "mine", amountPaise = 500)
    db.expenseDao().upsert(local.toEntity())
    db.outboxDao().insert(OutboxEntity(type = OutboxType.DeleteExpense, rowKey = "mine", base = null, createdAt = LocalDateTime.MIN))
    val localOnly = sampleExpense(id = "local-only")
    db.expenseDao().upsert(localOnly.toEntity())
    db.outboxDao().insert(OutboxEntity(type = OutboxType.DeleteExpense, rowKey = "local-only", base = null, createdAt = LocalDateTime.MIN))
    sheet.expenses += local.copy(amountPaise = 999)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), engine.sync("sheet-1"))

    assertEquals(500L, db.expenseDao().get("mine")?.amountPaise)
    assertEquals(localOnly, db.expenseDao().get("local-only")?.toDomain())
    assertTrue(sheet.writes.isEmpty())
  }

  @Test
  fun `an append that doesn't report its row re-reads before the next entry for that ID`() = runTest {
    val expense = sampleExpense(id = "twice")
    repository.add(expense)
    repository.add(expense.copy(amountPaise = 300))
    val gateway =
      object : com.shashi.expensetracker.domain.account.ExpenseSheetGateway by sheet {
        override suspend fun writeExpense(sheetId: String, expense: com.shashi.expensetracker.domain.expense.Expense, existingRow: Int?) =
          sheet.writeExpense(sheetId, expense, existingRow).let { if (existingRow == null) RowWriteResult.Written(null) else it }
      }

    assertEquals(SyncResult.Synced(pushed = 2, skipped = 0), newEngine(gateway).sync("sheet-1"))

    assertEquals(listOf(null, 2), sheet.writes.map { it.second })
    assertEquals(1, sheet.expenses.size)
  }

  @Test
  fun `with an empty outbox the run only pulls`() = runTest {
    sheet.expenses += sampleExpense(id = "pulled")

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), engine.sync("sheet-1"))

    assertEquals(listOf("pulled"), db.expenseDao().getAll().map { it.id })
    assertEquals(1, sheet.readExpensesCalls)
  }
}
