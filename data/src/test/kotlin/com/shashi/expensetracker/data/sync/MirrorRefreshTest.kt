package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.BudgetEntity
import com.shashi.expensetracker.data.budget.toDomain
import com.shashi.expensetracker.data.budget.toEntity
import com.shashi.expensetracker.data.category.CategoryEntity
import com.shashi.expensetracker.data.category.toDomain
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.inbox.toDomain
import com.shashi.expensetracker.data.inbox.toEntity
import com.shashi.expensetracker.data.mirrorRefresh
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.settings.toDomain
import com.shashi.expensetracker.data.settings.toEntity
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.ExpenseSource
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The five-table atomic diff (AD-18) against a real in-memory mirror. */
@RunWith(AndroidJUnit4::class)
class MirrorRefreshTest {

  private lateinit var db: AppDatabase
  private lateinit var refresh: MirrorRefresh

  private val october = YearMonth.of(2026, 10)
  private val on = LocalDateTime.of(2026, 11, 13, 9, 30)

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    refresh = db.mirrorRefresh()
  }

  @After fun tearDown() = db.close()

  private suspend fun pending(type: OutboxType, key: String) =
    db.outboxDao().insert(OutboxEntity(type = type, rowKey = key, base = null, createdAt = LocalDateTime.MIN))

  private suspend fun mirror() =
    SheetContents(
      expenses = db.expenseDao().getAll().map { it.toDomain() }.sortedBy { it.id },
      categories = db.categoryDao().getAll().map { it.toDomain() }.sortedBy { it.name },
      budgets = db.budgetDao().getAll().map { it.toDomain() }.sortedBy { it.category },
      dismissals = db.dismissalDao().getAll().map { it.toDomain() }.sortedBy { it.smsId },
      settings = db.settingDao().getAll().map { it.toDomain() }.sortedBy { it.key },
    )

  @Test
  fun `a new phone's empty mirror takes every row of all five tabs`() = runTest {
    val sheet =
      SheetContents(
        expenses = listOf(sampleExpense(id = "a"), sampleExpense(id = "b").copy(source = ExpenseSource.User)),
        categories = listOf(Category("Bills", "#3B82F6", "receipt_long"), Category("Food", "#F59E0B", "restaurant")),
        budgets = listOf(Budget(october, "Bills", 100_000), Budget(october, "Food", 500_000)),
        dismissals = listOf(Dismissal("h:abc", on)),
        settings = listOf(Setting("Currency", "INR"), Setting("Name", "Shashi")),
      )

    refresh(sheet)

    assertEquals(sheet, mirror())
  }

  @Test
  fun `rows edited in the Sheet take the new value and removed rows leave, in every table`() = runTest {
    db.expenseDao().upsertAll(listOf(sampleExpense(id = "edited", amountPaise = 1).toEntity(), sampleExpense(id = "gone").toEntity()))
    db.categoryDao().upsertAll(listOf(CategoryEntity("food", "#000000", ""), CategoryEntity("Old", "", "")))
    db.budgetDao().upsert(listOf(Budget(october, "food", 1).toEntity(), Budget(october, "Old", 1).toEntity()))
    db.dismissalDao().upsert(listOf(Dismissal("h:abc", on.minusDays(1)).toEntity(), Dismissal("h:gone", on).toEntity()))
    db.settingDao().upsert(listOf(Setting("Currency", "USD").toEntity(), Setting("Gone", "x").toEntity()))
    val sheet =
      SheetContents(
        expenses = listOf(sampleExpense(id = "edited", amountPaise = 999)),
        categories = listOf(Category("Food", "#F59E0B", "restaurant")),
        budgets = listOf(Budget(october, "Food", 500_000)),
        dismissals = listOf(Dismissal("h:abc", on)),
        settings = listOf(Setting("Currency", "INR")),
      )

    refresh(sheet)

    assertEquals(sheet, mirror())
  }

  @Test
  fun `a row with a pending entry is left as it is, whether the Sheet differs or lacks it`() = runTest {
    val mine = sampleExpense(id = "mine", amountPaise = 500)
    val localOnly = sampleExpense(id = "local-only")
    db.expenseDao().upsertAll(listOf(mine.toEntity(), localOnly.toEntity()))
    pending(OutboxType.DeleteExpense, "mine")
    pending(OutboxType.UpsertExpense, "local-only")
    db.categoryDao().upsertAll(listOf(CategoryEntity("Pets", "#10B981", "category")))
    pending(OutboxType.UpsertCategory, " pets ")
    db.budgetDao().upsert(listOf(BudgetEntity(october, "Food", 300_000), BudgetEntity(october, "Pets", 1_000)))
    pending(OutboxType.UpsertBudget, "2026-10|food")
    pending(OutboxType.UpsertBudget, "2026-10|Pets")
    db.dismissalDao().upsert(listOf(Dismissal("h:local", on).toEntity()))
    pending(OutboxType.AddDismissal, "h:local")
    db.settingDao().upsert(listOf(Setting("Currency", "USD").toEntity(), Setting("Name", "Me").toEntity()))
    pending(OutboxType.UpsertSetting, "Currency")
    pending(OutboxType.UpsertSetting, "Name")
    val before = mirror()

    refresh(
      SheetContents(
        expenses = listOf(mine.copy(amountPaise = 999)),
        budgets = listOf(Budget(october, "FOOD", 500_000)),
        settings = listOf(Setting("Currency", "INR")),
      )
    )

    assertEquals(before, mirror())
  }

  @Test
  fun `a key twice in a tab keeps the first row in sheet order`() = runTest {
    refresh(
      SheetContents(
        expenses = listOf(sampleExpense(id = "dup", amountPaise = 1), sampleExpense(id = "dup", amountPaise = 2)),
        categories = listOf(Category("Food", "#F59E0B", ""), Category(" food", "#000000", "")),
        budgets = listOf(Budget(october, "Food", 1), Budget(october, "FOOD", 2)),
        settings = listOf(Setting("Name", "first"), Setting("Name", "second")),
      )
    )

    val mirror = mirror()
    assertEquals(1L, mirror.expenses.single().amountPaise)
    assertEquals("#F59E0B", mirror.categories.single().color)
    assertEquals(1L, mirror.budgets.single().amountPaise)
    assertEquals("first", mirror.settings.single().value)
  }

  @Test
  fun `Categories that differ only in non-ASCII case stay two stable rows, as NOCASE sees them`() = runTest {
    db.categoryDao().upsertAll(listOf(CategoryEntity("Café", "#F59E0B", "")))
    val sheet = SheetContents(categories = listOf(Category("Café", "#F59E0B", ""), Category("CAFÉ", "#10B981", "")))

    refresh(sheet)
    refresh(sheet)

    assertEquals(
      listOf(Category("CAFÉ", "#10B981", ""), Category("Café", "#F59E0B", "")),
      mirror().categories,
    )
  }
}
