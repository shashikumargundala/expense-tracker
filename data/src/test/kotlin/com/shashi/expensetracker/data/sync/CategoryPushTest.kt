package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.toDomain
import com.shashi.expensetracker.data.budget.toEntity
import com.shashi.expensetracker.data.category.RoomCategoryRepository
import com.shashi.expensetracker.data.category.toDomain
import com.shashi.expensetracker.data.category.toEntity
import com.shashi.expensetracker.data.expense.RoomExpenseRepository
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.mirrorRefresh
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetTab
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The push of `UpsertCategory` and `DeleteCategory` (AD-4, AD-9): every row of story 3.5's matrix,
 * through a whole [SyncEngine] run against [FakeExpenseSheetGateway] and a real in-memory mirror.
 */
@RunWith(AndroidJUnit4::class)
class CategoryPushTest {

  private lateinit var db: AppDatabase
  private lateinit var categories: RoomCategoryRepository
  private lateinit var expenses: RoomExpenseRepository
  private val sheet = FakeExpenseSheetGateway()
  private val createdAt = LocalDateTime.of(2026, 11, 13, 9, 30)
  private val october = YearMonth.of(2026, 10)
  private val food = Category("Food", "#F59E0B", "restaurant")
  private val fun_ = Category("Fun", "#A855F7", "celebration")
  private val pets = Category("Pets", "#A855F7", "pets")

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    categories = RoomCategoryRepository(db, db.categoryDao(), db.expenseDao(), db.budgetDao(), db.outboxDao(), testClock)
    expenses = RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock)
  }

  @After fun tearDown() = db.close()

  private suspend fun sync() =
    SyncEngine(sheet, db.expenseDao(), db.categoryDao(), db.outboxDao(), db.mirrorRefresh(), testClock).sync("sheet-1")

  /** The Sheet and the mirror both hold [rows], as after a pull. */
  private suspend fun pulled(vararg rows: Category) {
    sheet.categories += rows
    db.categoryDao().upsertAll(rows.map { it.toEntity() })
  }

  private suspend fun mirror(): List<Category> = db.categoryDao().getAll().map { it.toDomain() }.sortedBy { it.name }

  private suspend fun outboxKeys() = db.outboxDao().getAll().map { it.rowKey }

  @Test
  fun `a new Category is appended with its three cells and the entry leaves`() = runTest {
    assertTrue(categories.upsert(pets))

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(pets to null), sheet.categoryWrites)
    assertEquals(listOf(pets), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(pets), mirror())
  }

  @Test
  fun `an upsert whose row equals its base updates Color and Icon in place`() = runTest {
    pulled(food, fun_)
    val edited = fun_.copy(color = "#10B981", icon = "sports_esports")
    assertTrue(categories.upsert(edited))

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(edited to 3), sheet.categoryWrites)
    assertEquals(listOf(food, edited), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(food, edited), mirror())
  }

  @Test
  fun `an upsert whose row changed in the Sheet writes nothing and the Sheet's value wins`() = runTest {
    pulled(fun_)
    assertTrue(categories.upsert(fun_.copy(icon = "sports_esports")))
    val handEdited = fun_.copy(color = "#000000")
    sheet.categories[0] = handEdited

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    assertEquals(listOf(handEdited), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(handEdited), mirror())
  }

  @Test
  fun `an upsert whose row was deleted in the Sheet writes nothing and the mirror row goes`() = runTest {
    pulled(fun_)
    assertTrue(categories.upsert(fun_.copy(icon = "sports_esports")))
    sheet.categories.clear()

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    assertTrue(sheet.categories.isEmpty())
    assertTrue(outboxKeys().isEmpty())
    assertTrue(mirror().isEmpty())
  }

  @Test
  fun `a create whose row already landed writes nothing`() = runTest {
    assertTrue(categories.upsert(pets))
    sheet.categories += pets

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    assertEquals(listOf(pets), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `an upsert whose row is invalid is skipped, kept and never appended`() = runTest {
    assertTrue(categories.upsert(pets))
    sheet.invalidCategoryNames += " pets"

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 1), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    assertTrue(sheet.categories.isEmpty())
    assertTrue(sheet.appendedCategories.isEmpty())
    assertEquals(listOf("Pets"), outboxKeys())
    assertEquals(listOf(pets), mirror())
  }

  @Test
  fun `a mirror Category whose Sheet row is invalid is kept by the refresh`() = runTest {
    db.categoryDao().upsertAll(listOf(fun_.toEntity()))
    sheet.invalidCategoryNames += "FUN"

    sync()

    assertEquals(listOf(fun_), mirror())
  }

  @Test
  fun `a pending new Category an Expense uses is appended by its push with the user's colour and icon`() = runTest {
    assertTrue(categories.upsert(pets))
    assertTrue(expenses.add(sampleExpense(id = "e").copy(category = "pets")))

    assertEquals(SyncResult.Synced(pushed = 2, skipped = 0), sync())

    assertTrue(sheet.appendedCategories.isEmpty())
    assertEquals(listOf(pets to null), sheet.categoryWrites)
    assertEquals(listOf(pets), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(pets), mirror())
  }

  @Test
  fun `a pending Category that can't push yet is still never appended with a default colour`() = runTest {
    assertTrue(categories.upsert(pets))
    sheet.expenses += sampleExpense(id = "e").copy(category = "Pets")
    sheet.writeCategoryResult = RowWriteResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(SyncResult.Failed(SyncFailure.Network), sync())
    sheet.writeCategoryResult = null
    sync()

    assertTrue(sheet.appendedCategories.isEmpty())
    assertEquals(listOf(pets), sheet.categories)
  }

  /** The Sheet and mirror for the delete cases: 3 Fun Expenses, 1 Food, 2 Fun Budgets, 1 Food. */
  private suspend fun funInUse() {
    pulled(food, fun_)
    val rows =
      listOf(
        sampleExpense(id = "a").copy(category = "Fun"),
        sampleExpense(id = "b"),
        sampleExpense(id = "c").copy(category = "fun"),
        sampleExpense(id = "d").copy(category = " FUN "),
      )
    sheet.expenses += rows
    db.expenseDao().upsertAll(rows.map { it.toEntity() })
    val budgets =
      listOf(Budget(october, "Fun", 1_000), Budget(october, "Food", 2_000), Budget(october.plusMonths(1), "fun", 3_000))
    sheet.budgets += budgets
    db.budgetDao().upsert(budgets.map { it.toEntity() })
  }

  @Test
  fun `delete with reassign rewrites the Expenses, deletes the Budgets and the Category, and the mirror agrees`() = runTest {
    funInUse()

    assertTrue(categories.delete("Fun", reassignTo = "Food"))
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(listOf(2, 4, 5) to "Food"), sheet.expenseCategoryWrites)
    assertEquals(listOf(SheetTab.Budgets to listOf(2, 4), SheetTab.Categories to listOf(3)), sheet.tabDeletes)
    assertTrue(sheet.expenses.none { it.category.trim().equals("fun", ignoreCase = true) })
    assertEquals(listOf("Food", "Food", "Food", "Food"), sheet.expenses.map { it.category })
    assertEquals(listOf(Budget(october, "Food", 2_000)), sheet.budgets)
    assertEquals(listOf(food), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(food), mirror())
    assertEquals(listOf("Food", "Food", "Food", "Food"), db.expenseDao().getAll().map { it.toDomain().category })
    assertEquals(listOf(Budget(october, "Food", 2_000)), db.budgetDao().getAll().map { it.toDomain() })
  }

  @Test
  fun `a delete rewrites an invalid Expense row's Category and deletes an invalid Budgets row of it`() = runTest {
    pulled(food, fun_)
    sheet.expenses += sampleExpense(id = "a").copy(category = "Fun")
    // An unreadable Amount: invalid, but its Category cell still says fun.
    sheet.expenseRows += ExpenseRowRead("bad", null, category = "fun")
    sheet.budgets += Budget(october, "Food", 2_000)
    // Budgets row 3, `2026-10 | Fun | 0`, is invalid; row 4 is another Category's.
    sheet.invalidBudgetKeys += "2026-10| Fun "
    sheet.invalidBudgetKeys += "2026-10|Food"
    assertTrue(categories.delete("Fun", reassignTo = "Food"))

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 2), sync())

    assertEquals(listOf(listOf(2, 3) to "Food"), sheet.expenseCategoryWrites)
    assertEquals(ExpenseRowRead("bad", null, category = "Food"), sheet.expenseRows[1])
    assertEquals(listOf(SheetTab.Budgets to listOf(3), SheetTab.Categories to listOf(3)), sheet.tabDeletes)
    assertEquals(listOf("2026-10|Food"), sheet.invalidBudgetKeys)
    assertEquals(listOf(Budget(october, "Food", 2_000)), sheet.budgets)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a delete skips the base check, even when the Category row changed in the Sheet`() = runTest {
    funInUse()
    assertTrue(categories.delete("Fun", reassignTo = "Food"))
    sheet.categories[1] = fun_.copy(color = "#000000")

    sync()

    assertEquals(listOf(food), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a delete whose Category row is already gone still rewrites the Expenses and drops the Budgets`() = runTest {
    funInUse()
    assertTrue(categories.delete("Fun", reassignTo = "Food"))
    sheet.categories.removeAt(1)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())

    assertEquals(listOf(listOf(2, 4, 5) to "Food"), sheet.expenseCategoryWrites)
    assertEquals(listOf(SheetTab.Budgets to listOf(2, 4)), sheet.tabDeletes)
    assertEquals(listOf(food), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
    assertEquals(listOf(food), mirror())
  }

  @Test
  fun `a bad reassign returns false and nothing changes or reaches the Sheet`() = runTest {
    funInUse()

    assertEquals(false, categories.delete("Fun", reassignTo = "fun"))
    assertEquals(false, categories.delete("Fun", reassignTo = "Pets"))
    sync()

    assertTrue(sheet.expenseCategoryWrites.isEmpty())
    assertTrue(sheet.tabDeletes.isEmpty())
    assertEquals(listOf(food, fun_), mirror())
  }

  @Test
  fun `a failed rewrite, delete or consent keeps the key's entries and skips the refresh, then the rerun applies`() = runTest {
    funInUse()
    assertTrue(categories.delete("Fun", reassignTo = "Food"))
    val consent = RowWriteResult.NeedsConsent(object : ConsentRequest {})

    sheet.expenseCategoriesResult = RowWriteResult.Failure(ExpenseSheetFailure.Network)
    assertEquals(SyncResult.Failed(SyncFailure.Network), sync())
    sheet.expenseCategoriesResult = null
    sheet.deleteRowsResults[SheetTab.Budgets] = consent
    assertEquals(SyncResult.Failed(SyncFailure.NeedsConsent), sync())
    sheet.deleteRowsResults.clear()
    sheet.deleteRowsResults[SheetTab.Categories] = RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
    assertEquals(SyncResult.Failed(SyncFailure.Unexpected), sync())

    assertEquals(listOf("Fun"), outboxKeys())
    // No refresh ran, so the mirror still shows the local delete.
    assertEquals(listOf(food), mirror())

    sheet.deleteRowsResults.clear()
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())
    assertEquals(listOf(food), sheet.categories)
    assertEquals(listOf(Budget(october, "Food", 2_000)), sheet.budgets)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a failed upsert write keeps the entry and the rerun applies`() = runTest {
    assertTrue(categories.upsert(pets))
    sheet.writeCategoryResult = RowWriteResult.NeedsConsent(object : ConsentRequest {})

    assertEquals(SyncResult.Failed(SyncFailure.NeedsConsent), sync())
    assertEquals(listOf("Pets"), outboxKeys())

    sheet.writeCategoryResult = null
    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), sync())
    assertEquals(listOf(pets), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `an upsert then a delete for a key is one delete, and an invalid row with no Category isn't rewritten`() = runTest {
    pulled(food, fun_)
    sheet.expenseRows += ExpenseRowRead("bad", null)
    assertTrue(categories.upsert(fun_.copy(color = "#10B981")))
    assertTrue(categories.delete("Fun", reassignTo = "Food"))

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 1), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    // The invalid row has no Category cell, so it isn't rewritten.
    assertTrue(sheet.expenseCategoryWrites.isEmpty())
    assertEquals(listOf(SheetTab.Categories to listOf(3)), sheet.tabDeletes)
    assertEquals(listOf(food), sheet.categories)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `a delete into a Category deleted in the same run follows it to its own target`() = runTest {
    pulled(food, fun_, pets)
    sheet.expenses += sampleExpense(id = "a").copy(category = "Fun")
    db.expenseDao().upsert(sampleExpense(id = "a").copy(category = "Fun").toEntity())
    assertTrue(categories.delete("Fun", reassignTo = "Pets"))
    assertTrue(categories.delete("Pets", reassignTo = "Food"))

    assertEquals(SyncResult.Synced(pushed = 2, skipped = 0), sync())

    assertEquals(listOf("Food"), sheet.expenses.map { it.category })
    assertEquals(listOf(food), sheet.categories)
    assertEquals(listOf(SheetTab.Categories to listOf(3, 4)), sheet.tabDeletes)
    assertTrue(outboxKeys().isEmpty())
  }

  @Test
  fun `an unreadable upsert base counts as changed in the Sheet`() = runTest {
    pulled(fun_)
    db.outboxDao().insert(OutboxEntity(type = OutboxType.UpsertCategory, rowKey = "Fun", base = "{", createdAt = createdAt))

    assertEquals(SyncResult.Synced(pushed = 0, skipped = 0), sync())

    assertTrue(sheet.categoryWrites.isEmpty())
    assertTrue(outboxKeys().isEmpty())
  }
}
