package com.shashi.expensetracker.data.budget

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budgets") suspend fun getAll(): List<BudgetEntity>

  @Upsert suspend fun upsert(budgets: List<BudgetEntity>)

  /** Deletes by key; the Category compares ignoring case (the column is `NOCASE`). */
  @Delete suspend fun delete(budgets: List<BudgetEntity>)

  /** Deletes every Budget of [category] (trimmed, ignoring ASCII case). Pass [category] trimmed. */
  @Query("DELETE FROM budgets WHERE trim(category) = :category COLLATE NOCASE")
  suspend fun deleteByCategory(category: String)
}
