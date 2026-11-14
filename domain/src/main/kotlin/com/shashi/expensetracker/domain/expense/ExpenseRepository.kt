package com.shashi.expensetracker.domain.expense

import kotlinx.coroutines.flow.Flow

/** Expenses in the local mirror (AD-3). Implemented in `:data`, which maps storage errors. */
interface ExpenseRepository {
  /**
   * Emits every Expense, newest first, each with its Pending Sync flag, and again on every change.
   * An unreadable mirror emits an empty list.
   */
  fun observe(): Flow<List<ExpenseListItem>>

  /**
   * Stores [expense] and its `UpsertExpense` outbox entry in one transaction. Returns false if
   * storage failed, in which case neither was written.
   */
  suspend fun add(expense: Expense): Boolean
}
