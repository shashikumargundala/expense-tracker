package com.shashi.expensetracker.data.category

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.BudgetEntity
import com.shashi.expensetracker.data.budget.toDomain
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.sync.CategoryBase
import com.shashi.expensetracker.data.sync.OutboxDao
import com.shashi.expensetracker.data.sync.OutboxEntity
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The transactional local Category writes (AD-3, AD-9). */
@RunWith(AndroidJUnit4::class)
class RoomCategoryRepositoryTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomCategoryRepository
  private val food = Category("Food", "#F59E0B", "restaurant")
  private val fun_ = Category("Fun", "#A855F7", "celebration")
  private val october = YearMonth.of(2026, 10)

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    repository = repository()
  }

  @After fun tearDown() = db.close()

  private fun repository(outbox: OutboxDao = db.outboxDao()) =
    RoomCategoryRepository(db, db.categoryDao(), db.expenseDao(), db.budgetDao(), outbox, testClock)

  private suspend fun categories() = repository.observe().first()

  @Test
  fun `a new Category is stored trimmed with an UpsertCategory whose base is absent`() = runTest {
    assertTrue(repository.upsert(Category(" Pets ", "#A855F7", "pets")))

    assertEquals(listOf(Category("Pets", "#A855F7", "pets")), categories())
    val entry = db.outboxDao().getAll().single()
    assertEquals(OutboxType.UpsertCategory, entry.type)
    assertEquals("Pets", entry.rowKey)
    assertNull(entry.base)
    assertNull(entry.payload)
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), entry.createdAt)
  }

  @Test
  fun `an edit of a pulled Category records the pulled values as its base`() = runTest {
    db.categoryDao().upsertAll(listOf(fun_.toEntity()))

    assertTrue(repository.upsert(fun_.copy(color = "#10B981")))

    assertEquals(CategoryBase.encode(fun_), db.outboxDao().getAll().single().base)
    assertEquals(listOf(Category("Fun", "#10B981", "celebration")), categories())
  }

  @Test
  fun `a blank name is refused`() = runTest {
    assertFalse(repository.upsert(Category("  ", "#000000", "")))
    assertTrue(db.outboxDao().getAll().isEmpty())
  }

  @Test
  fun `delete reassigns the Expenses, drops the Budgets and queues DeleteCategory in one transaction`() = runTest {
    db.categoryDao().upsertAll(listOf(food.toEntity(), fun_.toEntity()))
    val a = sampleExpense(id = "a").copy(category = "Fun")
    val b = sampleExpense(id = "b").copy(category = " FUN ")
    val c = sampleExpense(id = "c")
    db.expenseDao().upsertAll(listOf(a.toEntity(), b.toEntity(), c.toEntity()))
    db.budgetDao().upsert(listOf(BudgetEntity(october, "Fun", 1_000), BudgetEntity(october, "Food", 2_000)))

    assertTrue(repository.delete("fun", reassignTo = "FOOD"))

    assertEquals(listOf(food), categories())
    assertEquals(listOf("Food", "Food", "Food"), db.expenseDao().getAll().map { it.toDomain().category })
    assertEquals(listOf(Budget(october, "Food", 2_000)), db.budgetDao().getAll().map { it.toDomain() })
    val entry = db.outboxDao().getAll().single()
    assertEquals(OutboxType.DeleteCategory, entry.type)
    assertEquals("Fun", entry.rowKey)
    assertEquals("Food", entry.payload)
    assertEquals(CategoryBase.encode(fun_), entry.base)
  }

  @Test
  fun `delete into itself, into an unknown Category or of an unknown one changes nothing`() = runTest {
    db.categoryDao().upsertAll(listOf(food.toEntity(), fun_.toEntity()))
    db.expenseDao().upsert(sampleExpense(id = "a").copy(category = "Fun").toEntity())

    assertFalse(repository.delete("Fun", reassignTo = " fun "))
    assertFalse(repository.delete("Fun", reassignTo = "Pets"))
    assertFalse(repository.delete("Pets", reassignTo = "Food"))

    assertEquals(listOf(food, fun_), categories())
    assertEquals("Fun", db.expenseDao().get("a")?.category)
    assertTrue(db.outboxDao().getAll().isEmpty())
  }

  /** An outbox whose insert fails, as when the disk is full mid-transaction. */
  private class FailingOutboxDao(private val real: OutboxDao) : OutboxDao by real {
    override suspend fun insert(entry: OutboxEntity): Long = throw IllegalStateException("disk full")
  }

  @Test
  fun `a failed outbox write rolls back the whole delete and the upsert`() = runTest {
    db.categoryDao().upsertAll(listOf(food.toEntity(), fun_.toEntity()))
    db.expenseDao().upsert(sampleExpense(id = "a").copy(category = "Fun").toEntity())
    db.budgetDao().upsert(listOf(BudgetEntity(october, "Fun", 1_000)))
    val failing = repository(FailingOutboxDao(db.outboxDao()))

    assertFalse(failing.delete("Fun", reassignTo = "Food"))
    assertFalse(failing.upsert(Category("Pets", "", "")))

    assertEquals(listOf(food, fun_), categories())
    assertEquals("Fun", db.expenseDao().get("a")?.category)
    assertEquals(1, db.budgetDao().getAll().size)
    assertTrue(db.outboxDao().getAll().isEmpty())
  }
}
