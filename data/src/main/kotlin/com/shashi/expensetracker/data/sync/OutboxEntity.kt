package com.shashi.expensetracker.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * The closed set of outbox entry types (AD-3), stored by name. Each entry's `row_key` is its tab's
 * row key (AD-7): the Expense ID, the Category name, `yyyy-MM|Category` for a Budget, the SMS
 * fingerprint for a dismissal, or the Setting key.
 */
enum class OutboxType {
  UpsertExpense,
  DeleteExpense,
  UpsertCategory,
  DeleteCategory,
  UpsertBudget,
  AddDismissal,
  UpsertSetting;

  companion object {
    /** Entry types whose `row_key` is an Expense ID. */
    val expenseTypes = listOf(UpsertExpense, DeleteExpense)

    /** Entry types whose `row_key` is a Category name. */
    val categoryTypes = listOf(UpsertCategory, DeleteCategory)
  }
}

/**
 * One pending local change (AD-3), written in the same transaction as the change and removed only
 * after the API confirms the write. Entries apply in [id] order.
 *
 * [base] is a JSON text of the row's mapped values as last pulled, or null for "absent" (AD-4).
 */
@Entity(tableName = "outbox", indices = [Index("row_key")])
data class OutboxEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val type: OutboxType,
  @ColumnInfo(name = "row_key") val rowKey: String,
  val base: String?,
  @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
)
