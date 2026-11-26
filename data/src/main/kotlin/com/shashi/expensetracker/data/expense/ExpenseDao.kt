package com.shashi.expensetracker.data.expense

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
  /** Every Expense, newest first, flagged pending while an Expense outbox entry has its ID. */
  @Query(
    """
    SELECT e.*, EXISTS(
      SELECT 1 FROM outbox o
      WHERE o.row_key = e.id AND o.type IN ('UpsertExpense', 'DeleteExpense')
    ) AS pending
    FROM expenses e
    ORDER BY e.date DESC, e.id
    """
  )
  fun observeWithPending(): Flow<List<ExpenseWithPending>>

  @Query("SELECT * FROM expenses") suspend fun getAll(): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE id = :id") suspend fun get(id: String): ExpenseEntity?

  @Upsert suspend fun upsert(expense: ExpenseEntity)

  @Upsert suspend fun upsertAll(expenses: List<ExpenseEntity>)

  @Query("DELETE FROM expenses WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<String>)

  /**
   * Moves every Expense whose Category is [from] (trimmed, ignoring ASCII case, as `CategoryKey`
   * compares) to [to]. Pass [from] trimmed.
   */
  @Query("UPDATE expenses SET category = :to WHERE trim(category) = :from COLLATE NOCASE")
  suspend fun reassignCategory(from: String, to: String)
}
