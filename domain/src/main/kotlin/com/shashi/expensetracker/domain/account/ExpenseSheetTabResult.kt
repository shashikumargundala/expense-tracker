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
   * The tab's rows under its header. [skipped] counts invalid rows, such as a row with an
   * unreadable amount; they are left untouched in the Sheet (AD-7). A row with a blank key isn't
   * counted. [invalidKeys] holds the trimmed row key of each invalid row whose key could be read
   * (the ID, the Category name, `yyyy-MM|Category` for a Budget, the SMS ID or the Setting key, as
   * typed), so sync never appends a second row for a key the tab already has. [invalidRows] holds
   * the same keys with each invalid row's 1-based row number, in sheet order, so sync can delete
   * an invalid row that a pushed change removes (AD-9's Budgets cleanup).
   */
  data class Rows<T>(
    val rows: List<SheetRow<T>>,
    val skipped: Int = 0,
    val invalidKeys: Set<String> = emptySet(),
    val invalidRows: List<SheetRow<String>> = emptyList(),
  ) : TabReadResult<T>

  /** The Sheet was deleted (AD-20). */
  data object SheetMissing : TabReadResult<Nothing>

  /** Drive access needs the user's consent; background work stops here. */
  data class NeedsConsent(val request: ConsentRequest) : TabReadResult<Nothing>

  data class Failure(val reason: ExpenseSheetFailure) : TabReadResult<Nothing>
}

/** The five tabs of the Expense Sheet (AD-7). */
enum class SheetTab {
  Expenses,
  Categories,
  Budgets,
  Dismissed,
  Settings,
}

/** Outcome of writing one row with [ExpenseSheetGateway]. */
sealed interface RowWriteResult {
  /** The API confirmed the write. [row] is the row written, or null if the reply didn't say. */
  data class Written(val row: Int?) : RowWriteResult

  data object SheetMissing : RowWriteResult

  data class NeedsConsent(val request: ConsentRequest) : RowWriteResult

  data class Failure(val reason: ExpenseSheetFailure) : RowWriteResult
}
