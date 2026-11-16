package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.toDomain
import com.shashi.expensetracker.data.category.CategoryEntity
import com.shashi.expensetracker.data.expense.RoomExpenseRepository
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.inbox.toDomain
import com.shashi.expensetracker.data.mirrorRefresh
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.settings.toDomain
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.ExpenseSource
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A sync run against [FakeExpenseSheetGateway] and a real in-memory mirror: the push, the run order
 * and the pull matrix. The refresh's own diff cases are [MirrorRefreshTest]'s.
 */
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

  private fun newEngine(gateway: ExpenseSheetGateway = sheet) =
    SyncEngine(gateway, db.expenseDao(), db.outboxDao(), db.mirrorRefresh())

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
  fun `an append that doesn't report its row re-reads before the next entry for that ID`() = runTest {
    val expense = sampleExpense(id = "twice")
    repository.add(expense)
    repository.add(expense.copy(amountPaise = 300))
    val gateway =
      object : ExpenseSheetGateway by sheet {
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

  private val october = YearMonth.of(2026, 10)
  private val on = LocalDateTime.of(2026, 11, 13, 9, 30)

  private suspend fun mirrorBudgets() = db.budgetDao().getAll().map { it.toDomain() }

  @Test
  fun `a new phone pulls every tab into the empty mirror after repairing the layout`() = runTest {
    val expense = sampleExpense(id = "a").copy(source = ExpenseSource.User)
    sheet.expenses += expense
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.budgets += Budget(october, "Food", 500_000)
    sheet.dismissals += Dismissal("h:abc", on)
    sheet.settings += Setting("Currency", "INR")

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), engine.sync("sheet-1"))

    assertEquals(1, sheet.repairCalls)
    assertEquals(listOf(expense), db.expenseDao().getAll().map { it.toDomain() })
    assertEquals(listOf(CategoryEntity("Food", "#F59E0B", "restaurant")), db.categoryDao().getAll())
    assertEquals(listOf(Budget(october, "Food", 500_000)), mirrorBudgets())
    assertEquals(listOf(Dismissal("h:abc", on)), db.dismissalDao().getAll().map { it.toDomain() })
    assertEquals(listOf(Setting("Currency", "INR")), db.settingDao().getAll().map { it.toDomain() })
    assertTrue(sheet.appendedCategories.isEmpty())
  }

  @Test
  fun `an Expense or Budget takes its Category's own spelling in the mirror`() = runTest {
    sheet.expenses += sampleExpense(id = "hand").copy(category = "food", source = ExpenseSource.User)
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.budgets += Budget(october, "FOOD", 500_000)

    engine.sync("sheet-1")

    assertEquals("Food", db.expenseDao().get("hand")?.category)
    assertEquals(listOf(Budget(october, "Food", 500_000)), mirrorBudgets())
    assertTrue(sheet.appendedCategories.isEmpty())
  }

  @Test
  fun `an unknown Category is appended to Categories once, then mirrored`() = runTest {
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.expenses += sampleExpense(id = "a").copy(category = "Pets")
    sheet.expenses += sampleExpense(id = "b").copy(category = "pets")
    sheet.budgets += Budget(october, "Gym", 100_000)

    engine.sync("sheet-1")

    val added = listOf(Category("Pets", "#10B981", "category"), Category("Gym", "#E11D48", "category"))
    assertEquals(added, sheet.appendedCategories)
    assertEquals(
      setOf(CategoryEntity("Food", "#F59E0B", "restaurant")) + added.map { CategoryEntity(it.name, it.color, it.icon) },
      db.categoryDao().getAll().toSet(),
    )
    assertEquals(listOf("Pets"), db.expenseDao().getAll().map { it.category }.distinct())
  }

  @Test
  fun `a failed Category append fails the run with no refresh`() = runTest {
    sheet.expenses += sampleExpense(id = "a").copy(category = "Pets")
    sheet.appendCategoriesResult = RowWriteResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(SyncResult.Failed(SyncFailure.Network), engine.sync("sheet-1"))

    assertTrue(db.expenseDao().getAll().isEmpty())
    assertTrue(db.categoryDao().getAll().isEmpty())
  }

  @Test
  fun `a failed repair stops the run before any read, write or refresh`() = runTest {
    repository.add(sampleExpense(id = "kept"))
    sheet.expenses += sampleExpense(id = "pulled")
    val failures =
      listOf(
        RowWriteResult.Failure(ExpenseSheetFailure.Network) to SyncFailure.Network,
        RowWriteResult.NeedsConsent(object : ConsentRequest {}) to SyncFailure.NeedsConsent,
        RowWriteResult.SheetMissing to SyncFailure.SheetMissing,
      )

    failures.forEach { (repair, failure) ->
      sheet.repairResult = repair
      assertEquals(SyncResult.Failed(failure), engine.sync("sheet-1"))
    }

    assertEquals(0, sheet.readExpensesCalls)
    assertTrue(sheet.writes.isEmpty())
    assertEquals(listOf("kept"), db.expenseDao().getAll().map { it.id })
  }

  @Test
  fun `a failed read of any tab means no refresh at all`() = runTest {
    db.categoryDao().upsertAll(listOf(CategoryEntity("Old", "", "")))
    sheet.expenses += sampleExpense(id = "pulled")
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    val failure = TabReadResult.Failure(ExpenseSheetFailure.Unexpected)
    val scripts =
      listOf<FakeExpenseSheetGateway.() -> Unit>(
        { readBudgetsResult = failure },
        { readBudgetsResult = null; readDismissalsResult = failure },
        { readDismissalsResult = null; readSettingsResult = failure },
      )

    scripts.forEach { script ->
      sheet.script()
      assertEquals(SyncResult.Failed(SyncFailure.Unexpected), engine.sync("sheet-1"))
    }

    assertTrue(db.expenseDao().getAll().isEmpty())
    assertEquals(listOf("Old"), db.categoryDao().getAll().map { it.name })
  }

  @Test
  fun `invalid rows are counted across every tab`() = runTest {
    sheet.readExpensesResult = TabReadResult.Rows(listOf(SheetRow(2, sampleExpense(id = "ok"))), skipped = 2)
    sheet.readBudgetsResult = TabReadResult.Rows(emptyList(), skipped = 1)
    sheet.readCategoriesResult = TabReadResult.Rows(listOf(SheetRow(2, Category("Food", "", ""))), skipped = 0)
    sheet.readDismissalsResult = TabReadResult.Rows(emptyList(), skipped = 3)
    sheet.readSettingsResult = TabReadResult.Rows(emptyList(), skipped = 4)

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 10), engine.sync("sheet-1"))
    assertEquals(listOf("ok"), db.expenseDao().getAll().map { it.id })
  }

  @Test
  fun `an Expense saved after the reads, before the refresh, survives and stays pending`() = runTest {
    sheet.expenses += sampleExpense(id = "pulled").copy(category = "Pets")
    val late = sampleExpense(id = "late")
    // The append runs after every read and push, just before the refresh.
    val gated =
      object : ExpenseSheetGateway by sheet {
        override suspend fun appendCategories(sheetId: String, categories: List<Category>): RowWriteResult {
          repository.add(late)
          return sheet.appendCategories(sheetId, categories)
        }
      }

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), newEngine(gated).sync("sheet-1"))

    assertEquals(mapOf("pulled" to false, "late" to true), pending())
    assertEquals(late, db.expenseDao().get("late")?.toDomain())
    assertNull(sheet.expenses.firstOrNull { it.id == "late" })
  }
}
