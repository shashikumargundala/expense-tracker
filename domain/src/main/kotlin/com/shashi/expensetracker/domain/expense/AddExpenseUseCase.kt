package com.shashi.expensetracker.domain.expense

import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of [AddExpenseUseCase]. */
sealed interface AddExpenseResult {
  data class Added(val expense: Expense) : AddExpenseResult

  data class Failure(val reason: AddExpenseFailure) : AddExpenseResult
}

/** Why an Expense was not added. Nothing is written for any of these. */
enum class AddExpenseFailure {
  /** The amount is zero or negative. */
  InvalidAmount,

  /** No Category was chosen. */
  NoCategory,

  /** The mirror could not store the Expense. */
  StorageFailed,
}

/**
 * Adds an app-created Expense: validates it, then gives it a new lowercase UUID (AD-5), Source
 * [ExpenseSource.App] and the date "now" from the injected [Clock], truncated to the minute (AD-19).
 * The repository stores it with its outbox entry in one transaction (AD-3). Never throws, except
 * for coroutine cancellation.
 */
class AddExpenseUseCase
@Inject
constructor(private val expenses: ExpenseRepository, private val clock: Clock) {

  suspend operator fun invoke(
    amountPaise: Long,
    category: String,
    description: String,
  ): AddExpenseResult {
    if (amountPaise <= 0) return AddExpenseResult.Failure(AddExpenseFailure.InvalidAmount)
    val name = category.trim()
    if (name.isEmpty()) return AddExpenseResult.Failure(AddExpenseFailure.NoCategory)
    val expense =
      Expense(
        id = UUID.randomUUID().toString().lowercase(),
        date = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES),
        amountPaise = amountPaise,
        category = name,
        description = description.trim(),
        source = ExpenseSource.App,
      )
    val saved =
      try {
        expenses.add(expense)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        false
      }
    return if (saved) {
      AddExpenseResult.Added(expense)
    } else {
      AddExpenseResult.Failure(AddExpenseFailure.StorageFailed)
    }
  }
}
