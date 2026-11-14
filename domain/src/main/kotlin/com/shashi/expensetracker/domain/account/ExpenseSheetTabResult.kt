package com.shashi.expensetracker.domain.account

/** One row read from an Expense Sheet tab, with its 1-based [row] number in that tab. */
data class SheetRow<out T>(val row: Int, val value: T)

/**
 * Outcome of reading one tab with [ExpenseSheetGateway]. Rows are in sheet order. A read is only
 * valid for the sync run that made it: rows move when the Sheet is sorted, so a row number is never
 * kept across runs (AD-4).
 */
sealed interface TabReadResult<out T> {
  /**
   * The tab's valid rows under its header. [skipped] counts rows that couldn't be read (for example
   * a blank ID or an unreadable amount); they are left untouched in the Sheet (AD-7).
   */
  data class Rows<T>(val rows: List<SheetRow<T>>, val skipped: Int = 0) : TabReadResult<T>

  /** The Sheet was deleted (AD-20). */
  data object SheetMissing : TabReadResult<Nothing>

  /** Drive access needs the user's consent; background work stops here. */
  data class NeedsConsent(val request: ConsentRequest) : TabReadResult<Nothing>

  data class Failure(val reason: ExpenseSheetFailure) : TabReadResult<Nothing>
}

/** Outcome of writing one row with [ExpenseSheetGateway]. */
sealed interface RowWriteResult {
  /** The API confirmed the write. [row] is the row written, or null if the reply didn't say. */
  data class Written(val row: Int?) : RowWriteResult

  data object SheetMissing : RowWriteResult

  data class NeedsConsent(val request: ConsentRequest) : RowWriteResult

  data class Failure(val reason: ExpenseSheetFailure) : RowWriteResult
}
