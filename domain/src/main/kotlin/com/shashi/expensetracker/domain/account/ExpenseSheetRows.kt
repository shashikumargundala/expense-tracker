package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.expense.Expense

/**
 * One non-blank row of the Expenses tab as a pull reads it, with what AD-5's ID repair needs.
 *
 * [id] is the trimmed ID cell, or null when it is blank. [expense] is the parsed row, or null when
 * its Date, Amount or Category is missing or unreadable: an invalid row (AD-7), which is never
 * repaired, deleted or written, and whose [id] still counts as present in the Sheet. For a valid row
 * with a blank ID, [expense] has an empty `id`. [sourceBlank] is whether the Source cell was blank,
 * which [expense] reads as `User`.
 */
data class ExpenseRowRead(val id: String?, val expense: Expense?, val sourceBlank: Boolean = false) {
  /** A row the app can mirror: valid, with an ID. */
  val keyed: Expense?
    get() = expense?.takeIf { id != null }
}

/**
 * The mapped cells AD-5's repair writes into Expenses [row] (1-based, from this run's read). A
 * non-null [id] is written to ID; [sourceUser] writes `User` to Source; [clearSmsId] blanks SMS ID.
 * No other cell is touched.
 */
data class ExpenseRowFix(
  val row: Int,
  val id: String? = null,
  val sourceUser: Boolean = false,
  val clearSmsId: Boolean = false,
)
