package com.shashi.expensetracker.domain.expense

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Emits the mirror's Expenses, newest first, with their Pending Sync flag. Never throws. */
class ObserveExpensesUseCase @Inject constructor(private val expenses: ExpenseRepository) {
  operator fun invoke(): Flow<List<ExpenseListItem>> = expenses.observe()
}
