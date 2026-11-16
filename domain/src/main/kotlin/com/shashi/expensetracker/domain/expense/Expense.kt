package com.shashi.expensetracker.domain.expense

import java.time.LocalDateTime

/**
 * One Expense, as stored in the mirror and the Sheet's Expenses tab (AD-7).
 *
 * [id] is a random lowercase UUID (AD-5). [date] is minute-precision and has no time zone (AD-19).
 * [amountPaise] is whole paise (AD-6). [category] is the Category's name (AD-9). [smsId] is the SMS
 * fingerprint for an Expense added from the Inbox, otherwise null.
 */
data class Expense(
  val id: String,
  val date: LocalDateTime,
  val amountPaise: Long,
  val category: String,
  val description: String,
  val source: ExpenseSource,
  val smsId: String? = null,
)

/** Who created an Expense. A blank Source in the Sheet reads as [User] (AD-7). */
enum class ExpenseSource {
  /** Created in the app. */
  App,

  /** Typed into the Sheet by hand. */
  User,
}

/** An Expense for a list, with whether it still has an outbox entry (Pending Sync, AD-3). */
data class ExpenseListItem(val expense: Expense, val pending: Boolean)
