package com.shashi.expensetracker.data.expense

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime

/**
 * A mirrored Expenses row (AD-3). There is no `synced` column: an Expense is pending while an
 * outbox entry has its ID as `row_key`.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey val id: String,
  val date: LocalDateTime,
  @ColumnInfo(name = "amount_paise") val amountPaise: Long,
  val category: String,
  val description: String,
  val source: ExpenseSource,
  @ColumnInfo(name = "sms_id") val smsId: String?,
)

/** An [ExpenseEntity] with whether it still has an outbox entry. */
data class ExpenseWithPending(@Embedded val expense: ExpenseEntity, val pending: Boolean)

fun ExpenseEntity.toDomain(): Expense =
  Expense(
    id = id,
    date = date,
    amountPaise = amountPaise,
    category = category,
    description = description,
    source = source,
    smsId = smsId,
  )

fun Expense.toEntity(): ExpenseEntity =
  ExpenseEntity(
    id = id,
    date = date,
    amountPaise = amountPaise,
    category = category,
    description = description,
    source = source,
    smsId = smsId,
  )
