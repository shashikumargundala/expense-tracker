package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting

/**
 * The tab reads and row writes behind [SheetsExpenseSheetGateway] (AD-7). Reads ask for
 * `UNFORMATTED_VALUE` with `SERIAL_NUMBER` dates; writes use `USER_ENTERED`. Columns are found by
 * header name in a fresh read of the header row, and only mapped cells are written, so unknown
 * columns keep their values. Nothing is logged here.
 */
internal class SheetTabs(private val sheets: SheetsService) {

  suspend fun readExpenses(sheetId: String): TabReadResult<Expense> =
    readTab(sheetId, ExpenseSheetLayout.EXPENSES, ExpenseRowMapping.requiredHeaders, ExpenseRowMapping::expense)

  suspend fun readCategories(sheetId: String): TabReadResult<Category> =
    readTab(sheetId, ExpenseSheetLayout.CATEGORIES, CategoryRowMapping.requiredHeaders, CategoryRowMapping::category)

  suspend fun readBudgets(sheetId: String): TabReadResult<Budget> =
    readTab(sheetId, ExpenseSheetLayout.BUDGETS, BudgetRowMapping.requiredHeaders, BudgetRowMapping::budget)

  suspend fun readDismissals(sheetId: String): TabReadResult<Dismissal> =
    readTab(sheetId, ExpenseSheetLayout.DISMISSED, DismissalRowMapping.requiredHeaders, DismissalRowMapping::dismissal)

  suspend fun readSettings(sheetId: String): TabReadResult<Setting> =
    readTab(sheetId, ExpenseSheetLayout.SETTINGS, SettingRowMapping.requiredHeaders, SettingRowMapping::setting)

  /**
   * Updates the mapped cells of [existingRow], except Source, with one `values.batchUpdate`; or,
   * with no row, appends one row covering the mapped columns.
   */
  suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult {
    val tab = ExpenseSheetLayout.EXPENSES
    val columns =
      when (val header = headerColumns(sheetId, tab, ExpenseRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    val cells = mapped(columns, ExpenseRowMapping.cells(expense, withSource = existingRow == null))
    if (existingRow == null) return appendRows(sheetId, tab, listOf(cells))
    val data =
      cells.map { (column, value) ->
        ValueRange(range = "$tab!${SheetValues.columnLetter(column)}$existingRow", values = listOf(listOf(value)))
      }
    val request = BatchUpdateValuesRequest(valueInputOption = BatchUpdateValuesRequest.USER_ENTERED, data = data)
    return when (val r = googleApiCall { sheets.batchUpdateValues(sheetId, request) }) {
      is ApiResult.Failure -> r.reason.toWriteResult()
      is ApiResult.Success -> RowWriteResult.Written(existingRow)
    }
  }

  /** Appends [categories] as rows of the Categories tab with one `values.append`, Name, Color and Icon only. */
  suspend fun appendCategories(sheetId: String, categories: List<Category>): RowWriteResult {
    if (categories.isEmpty()) return RowWriteResult.Written(null)
    val tab = ExpenseSheetLayout.CATEGORIES
    val columns =
      when (val header = headerColumns(sheetId, tab, CategoryRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    return appendRows(sheetId, tab, categories.map { mapped(columns, CategoryRowMapping.cells(it)) })
  }

  /** [cells] (header name to value) as (column, value) pairs, dropping headers the tab lacks. */
  private fun mapped(columns: Map<String, Int>, cells: Map<String, String>): List<Pair<Int, String>> =
    cells.mapNotNull { (name, value) -> SheetValues.column(columns, name)?.let { it to value } }

  /** Appends one row per entry of [rows], each set only in its mapped columns. */
  private suspend fun appendRows(sheetId: String, tab: String, rows: List<List<Pair<Int, String>>>): RowWriteResult {
    val width = rows.maxOf { cells -> cells.maxOf { it.first } } + 1
    val values =
      rows.map { cells -> MutableList<Any?>(width) { "" }.also { row -> cells.forEach { (column, value) -> row[column] = value } } }
    val range = "$tab!A1"
    return when (
      val r =
        googleApiCall {
          sheets.appendValues(
            spreadsheetId = sheetId,
            range = range,
            body = ValueRange(range = range, values = values),
            valueInputOption = BatchUpdateValuesRequest.USER_ENTERED,
            insertDataOption = INSERT_ROWS,
          )
        }
    ) {
      is ApiResult.Failure -> r.reason.toWriteResult()
      is ApiResult.Success -> RowWriteResult.Written(SheetValues.firstRow(r.value.updates?.updatedRange))
    }
  }

  private sealed interface HeaderRead {
    data class Columns(val columns: Map<String, Int>) : HeaderRead

    data class Failed(val result: RowWriteResult) : HeaderRead
  }

  /** Row 1 of [tab] as columns; a failure if it can't be read or lacks a header in [required]. */
  private suspend fun headerColumns(sheetId: String, tab: String, required: List<String>): HeaderRead {
    val header =
      when (val read = get(sheetId, "$tab!1:1")) {
        is ApiResult.Failure -> return HeaderRead.Failed(read.reason.toWriteResult())
        is ApiResult.Success -> read.value.values.firstOrNull().orEmpty()
      }
    val columns = SheetValues.columns(header)
    if (!SheetValues.hasAll(columns, required)) return HeaderRead.Failed(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected))
    return HeaderRead.Columns(columns)
  }

  /**
   * Reads all of [tab]. Row 1 is the header and must have every name in [required]; sync repairs
   * the layout first, so a header still missing fails the read and no mirror is refreshed from a tab
   * it can't understand. Blank rows and rows with a blank key are left out; rows [parse] finds
   * invalid are left out and counted as skipped.
   */
  private suspend fun <T> readTab(
    sheetId: String,
    tab: String,
    required: List<String>,
    parse: (List<Any?>, Map<String, Int>) -> ParsedRow<T>,
  ): TabReadResult<T> {
    val values =
      when (val read = get(sheetId, tab)) {
        is ApiResult.Failure -> return read.reason.toReadResult()
        is ApiResult.Success -> read.value.values
      }
    val columns = SheetValues.columns(values.firstOrNull().orEmpty())
    if (!SheetValues.hasAll(columns, required)) return TabReadResult.Failure(ExpenseSheetFailure.Unexpected)
    var skipped = 0
    val rows =
      values.drop(1).mapIndexedNotNull { index, row ->
        if (SheetValues.isBlank(row)) return@mapIndexedNotNull null
        when (val parsed = parse(row, columns)) {
          // values[0] is row 1, so values.drop(1)[index] is row index + 2.
          is ParsedRow.Valid -> SheetRow(row = index + 2, value = parsed.value)
          ParsedRow.Invalid -> {
            skipped++
            null
          }
          ParsedRow.Unkeyed -> null
        }
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

  private companion object {
    const val INSERT_ROWS = "INSERT_ROWS"
  }
}
