package com.shashi.expensetracker.data.budget

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.YearMonth

/** A mirrored Budgets row, keyed by ([month], [category]) and stored sparse (AD-8). */
@Entity(tableName = "budgets", primaryKeys = ["month", "category"])
data class BudgetEntity(
  val month: YearMonth,
  @ColumnInfo(collate = ColumnInfo.NOCASE) val category: String,
  @ColumnInfo(name = "amount_paise") val amountPaise: Long,
)
