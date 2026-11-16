package com.shashi.expensetracker.data.budget

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.shashi.expensetracker.domain.budget.Budget
import java.time.YearMonth

/** A mirrored Budgets row, keyed by ([month], [category]) and stored sparse (AD-8). */
@Entity(tableName = "budgets", primaryKeys = ["month", "category"])
data class BudgetEntity(
  val month: YearMonth,
  @ColumnInfo(collate = ColumnInfo.NOCASE) val category: String,
  @ColumnInfo(name = "amount_paise") val amountPaise: Long,
)

fun BudgetEntity.toDomain(): Budget = Budget(month = month, category = category, amountPaise = amountPaise)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(month = month, category = category, amountPaise = amountPaise)
