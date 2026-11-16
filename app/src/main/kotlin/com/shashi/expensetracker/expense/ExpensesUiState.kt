package com.shashi.expensetracker.expense

import com.shashi.expensetracker.domain.expense.ExpenseListItem

/**
 * State of [ExpensesScreen]: the mirror's Expenses, newest first, each with its Pending Sync flag,
 * and the Category names the form offers. [loaded] is false until the mirror has been read once.
 * [saveFailed] is set when the last add couldn't be stored.
 */
data class ExpensesUiState(
  val loaded: Boolean = false,
  val expenses: List<ExpenseListItem> = emptyList(),
  val categories: List<String> = emptyList(),
  val saveFailed: Boolean = false,
)
