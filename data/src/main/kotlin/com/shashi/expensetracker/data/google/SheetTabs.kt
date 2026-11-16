package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense

/**
 * The tab reads and the Expenses row write behind [SheetsExpenseSheetGateway] (AD-7). Reads ask
 * for `UNFORMATTED_VALUE` with `SERIAL_NUMBER` dates; writes use `USER_ENTERED`. Columns are found
 * by header name in a fresh read of the header row, and only mapped cells are written, so unknown
 * columns keep their values. Nothing is logged here.
 */
internal class SheetTabs(private val sheets: SheetsService) {

  suspend fun readExpenses(sheetId: String): TabReadResult<Expense> =
    readTab(sheetId, ExpenseSheetLayout.EXPENSES, ExpenseRowMapping.requiredExpenseHeaders, ExpenseRowMapping::expense)

  suspend fun readCategories(sheetId: String): TabReadResult<Category> =
    readTab(sheetId, ExpenseSheetLayout.CATEGORIES, listOf(ExpenseSheetLayout.NAME), ExpenseRowMapping::category)

  /**
   * Updates the mapped cells of [existingRow], except Source, with one `values.batchUpdate`; or,
   * with no row, appends one row covering the mapped columns.
   */
  suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult {
    val tab = ExpenseSheetLayout.EXPENSES
    val header =
      when (val read = get(sheetId, "$tab!1:1")) {
        is ApiResult.Failure -> return read.reason.toWriteResult()
        is ApiResult.Success -> read.value.values.firstOrNull().orEmpty()
      }
    val columns = ExpenseRowMapping.columns(header)
    if (!ExpenseRowMapping.hasAll(columns, ExpenseRowMapping.requiredExpenseHeaders)) {
      return RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
    }
    val cells =
      ExpenseRowMapping.cells(expense, withSource = existingRow == null).mapNotNull { (name, value) ->
        ExpenseRowMapping.column(columns, name)?.let { it to value }
      }
    return if (existingRow != null) {
      val data =
        cells.map { (column, value) ->
          ValueRange(range = "$tab!${ExpenseRowMapping.columnLetter(column)}$existingRow", values = listOf(listOf(value)))
        }
      val request = BatchUpdateValuesRequest(valueInputOption = BatchUpdateValuesRequest.USER_ENTERED, data = data)
      when (val r = googleApiCall { sheets.batchUpdateValues(sheetId, request) }) {
        is ApiResult.Failure -> r.reason.toWriteResult()
        is ApiResult.Success -> RowWriteResult.Written(existingRow)
      }
    } else {
      val row = MutableList<Any?>(cells.maxOf { it.first } + 1) { "" }
      cells.forEach { (column, value) -> row[column] = value }
      val range = "$tab!A1"
      when (
        val r =
          googleApiCall {
            sheets.appendValues(
              spreadsheetId = sheetId,
              range = range,
              body = ValueRange(range = range, values = listOf(row)),
              valueInputOption = BatchUpdateValuesRequest.USER_ENTERED,
              insertDataOption = INSERT_ROWS,
            )
          }
      ) {
        is ApiResult.Failure -> r.reason.toWriteResult()
        is ApiResult.Success -> RowWriteResult.Written(ExpenseRowMapping.firstRow(r.value.updates?.updatedRange))
      }
    }
  }

  /**
   * Reads all of [tab]. Row 1 is the header and must have every name in [required]; without it the
   * read fails, so a mirror is never refreshed from a tab it can't understand. Blank rows are
   * ignored; rows [parse] rejects are counted as skipped.
   */
  private suspend fun <T> readTab(
    sheetId: String,
    tab: String,
    required: List<String>,
    parse: (List<Any?>, Map<String, Int>) -> T?,
  ): TabReadResult<T> {
    val values =
      when (val read = get(sheetId, tab)) {
        is ApiResult.Failure -> return read.reason.toReadResult()
        is ApiResult.Success -> read.value.values
      }
    val columns = ExpenseRowMapping.columns(values.firstOrNull().orEmpty())
    if (!ExpenseRowMapping.hasAll(columns, required)) return TabReadResult.Failure(ExpenseSheetFailure.Unexpected)
    var skipped = 0
    val rows =
      values.drop(1).mapIndexedNotNull { index, row ->
        if (ExpenseRowMapping.isBlank(row)) return@mapIndexedNotNull null
        // values[0] is row 1, so values.drop(1)[index] is row index + 2.
        val value = parse(row, columns) ?: run { skipped++; return@mapIndexedNotNull null }
        SheetRow(row = index + 2, value = value)
      }
    return TabReadResult.Rows(rows, skipped)
  }

  private suspend fun get(sheetId: String, range: String): ApiResult<ValueRange> = googleApiCall {
    sheets.getValues(
      spreadsheetId = sheetId,
      range = range,
      valueRenderOption = ValueRenderOption.UNFORMATTED_VALUE,
      dateTimeRenderOption = ValueRenderOption.SERIAL_NUMBER,
    )
  }

  private fun ApiFailure.toReadResult(): TabReadResult<Nothing> =
    when (this) {
      is ApiFailure.NeedsUserConsent -> TabReadResult.NeedsConsent(request)
      is ApiFailure.Access -> TabReadResult.Failure(ExpenseSheetFailure.Access)
      ApiFailure.Network -> TabReadResult.Failure(ExpenseSheetFailure.Network)
      ApiFailure.Http(NOT_FOUND) -> TabReadResult.SheetMissing
      is ApiFailure.Http,
      ApiFailure.Unexpected -> TabReadResult.Failure(ExpenseSheetFailure.Unexpected)
    }

  private fun ApiFailure.toWriteResult(): RowWriteResult =
    when (val read = toReadResult()) {
      is TabReadResult.NeedsConsent -> RowWriteResult.NeedsConsent(read.request)
      is TabReadResult.Failure -> RowWriteResult.Failure(read.reason)
      TabReadResult.SheetMissing -> RowWriteResult.SheetMissing
      is TabReadResult.Rows -> RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
    }

  private companion object {
    /** Sheets answers 404 for a deleted spreadsheet. */
    const val NOT_FOUND = 404
    const val INSERT_ROWS = "INSERT_ROWS"
  }
}
