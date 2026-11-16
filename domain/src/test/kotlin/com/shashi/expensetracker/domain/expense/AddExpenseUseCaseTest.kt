package com.shashi.expensetracker.domain.expense

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddExpenseUseCaseTest {

  private class FakeExpenseRepository : ExpenseRepository {
    val added = mutableListOf<Expense>()
    var fail = false
    var throws = false

    override fun observe(): Flow<List<ExpenseListItem>> = MutableStateFlow(emptyList())

    override suspend fun add(expense: Expense): Boolean {
      if (throws) throw IllegalStateException("disk")
      if (fail) return false
      added += expense
      return true
    }
  }

  private val repository = FakeExpenseRepository()

  // 2026-11-13 09:30:42.5 in Kolkata.
  private val clock = Clock.fixed(Instant.parse("2026-11-13T04:00:42.500Z"), ZoneId.of("Asia/Kolkata"))
  private val addExpense = AddExpenseUseCase(repository, clock)

  @Test
  fun `a valid Expense gets a lowercase UUID, Source App and now to the minute`() = runTest {
    val result = addExpense(amountPaise = 12_000, category = " Food ", description = " Chai ")

    val expense = (result as AddExpenseResult.Added).expense
    assertEquals(listOf(expense), repository.added)
    assertTrue(expense.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), expense.date)
    assertEquals(12_000L, expense.amountPaise)
    assertEquals("Food", expense.category)
    assertEquals("Chai", expense.description)
    assertEquals(ExpenseSource.App, expense.source)
    assertEquals(null, expense.smsId)
  }

  @Test
  fun `each Expense gets its own ID`() = runTest {
    addExpense(100, "Food", "")
    addExpense(100, "Food", "")

    assertEquals(2, repository.added.map { it.id }.toSet().size)
  }

  @Test
  fun `zero or negative amounts are rejected and nothing is written`() = runTest {
    assertEquals(AddExpenseResult.Failure(AddExpenseFailure.InvalidAmount), addExpense(0, "Food", "Chai"))
    assertEquals(AddExpenseResult.Failure(AddExpenseFailure.InvalidAmount), addExpense(-5, "Food", "Chai"))
    assertTrue(repository.added.isEmpty())
  }

  @Test
  fun `a blank Category is rejected and nothing is written`() = runTest {
    assertEquals(AddExpenseResult.Failure(AddExpenseFailure.NoCategory), addExpense(100, "  ", "Chai"))
    assertTrue(repository.added.isEmpty())
  }

  @Test
  fun `a failed or throwing store is StorageFailed`() = runTest {
    repository.fail = true
    assertEquals(AddExpenseResult.Failure(AddExpenseFailure.StorageFailed), addExpense(100, "Food", ""))

    repository.throws = true
    assertEquals(AddExpenseResult.Failure(AddExpenseFailure.StorageFailed), addExpense(100, "Food", ""))
  }
}
