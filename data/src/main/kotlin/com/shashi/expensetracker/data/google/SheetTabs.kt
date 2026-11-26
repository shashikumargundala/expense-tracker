package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.SheetTab
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

  /**
   * Every non-blank Expenses row, blank IDs and invalid rows included; invalid rows are counted and
   * their IDs are the read's invalid keys.
   */
  suspend fun readExpenses(sheetId: String): TabReadResult<ExpenseRowRead> =
    when (
      val read =
        readTab(sheetId, ExpenseSheetLayout.EXPENSES, ExpenseRowMapping.requiredHeaders, { _, _ -> null }) { row, columns ->
          ParsedRow.Valid(ExpenseRowMapping.row(row, columns))
        }
    ) {
      is TabReadResult.Rows -> {
        val invalid = read.rows.filter { it.value.expense == null }
        read.copy(
          skipped = invalid.size,
          invalidKeys = invalid.mapNotNullTo(LinkedHashSet()) { it.value.id },
          invalidRows = invalid.mapNotNull { row -> row.value.id?.let { SheetRow(row.row, it) } },
        )
      }
      else -> read
    }

  /**
   * Every keyed Categories row. [CategoryRowMapping.category] never finds a row invalid today (Color
   * and Icon may be blank), so this read's invalid keys are always empty in the real gateway; sync's
   * handling of invalid Categories rows is kept for a mapping that adds validation.
   */
  suspend fun readCategories(sheetId: String): TabReadResult<Category> =
    readTab(
      sheetId,
      ExpenseSheetLayout.CATEGORIES,
      CategoryRowMapping.requiredHeaders,
      { row, columns -> SheetValues.text(SheetValues.cell(row, columns, ExpenseSheetLayout.NAME)) },
      CategoryRowMapping::category,
    )

  suspend fun readBudgets(sheetId: String): TabReadResult<Budget> =
    readTab(sheetId, ExpenseSheetLayout.BUDGETS, BudgetRowMapping.requiredHeaders, BudgetRowMapping::key, BudgetRowMapping::budget)

  suspend fun readDismissals(sheetId: String): TabReadResult<Dismissal> =
    readTab(
      sheetId,
      ExpenseSheetLayout.DISMISSED,
      DismissalRowMapping.requiredHeaders,
      { row, columns -> SheetValues.text(SheetValues.cell(row, columns, ExpenseSheetLayout.SMS_ID)) },
      DismissalRowMapping::dismissal,
    )

  suspend fun readSettings(sheetId: String): TabReadResult<Setting> =
    readTab(
      sheetId,
      ExpenseSheetLayout.SETTINGS,
      SettingRowMapping.requiredHeaders,
      { row, columns -> SheetValues.text(SheetValues.cell(row, columns, ExpenseSheetLayout.KEY)) },
      SettingRowMapping::setting,
    )

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
    return updateRow(sheetId, tab, existingRow, cells)
  }

  /**
   * Updates the Name, Color and Icon cells of [existingRow] with one `values.batchUpdate`; or, with
   * no row, appends one row with only those cells.
   */
  suspend fun writeCategory(sheetId: String, category: Category, existingRow: Int?): RowWriteResult {
    val tab = ExpenseSheetLayout.CATEGORIES
    val columns =
      when (val header = headerColumns(sheetId, tab, CategoryRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    val cells = mapped(columns, CategoryRowMapping.cells(category))
    if (existingRow == null) return appendRows(sheetId, tab, listOf(cells))
    return updateRow(sheetId, tab, existingRow, cells)
  }

  /** Writes [category], as text, into the Category cell of each Expenses row in [rows], with one `values.batchUpdate`. */
  suspend fun writeExpenseCategories(sheetId: String, rows: List<Int>, category: String): RowWriteResult {
    if (rows.isEmpty()) return RowWriteResult.Written(null)
    val tab = ExpenseSheetLayout.EXPENSES
    val columns =
      when (val header = headerColumns(sheetId, tab, ExpenseRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    val column = SheetValues.column(columns, ExpenseSheetLayout.CATEGORY) ?: return RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
    val value = SheetValues.asText(category)
    val data =
      rows.distinct().sorted().map { row ->
        ValueRange(range = "$tab!${SheetValues.columnLetter(column)}$row", values = listOf(listOf(value)))
      }
    return batchUpdate(sheetId, data, written = null)
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

  /**
   * Writes the ID, Source and SMS ID cells that [fixes] name with one `USER_ENTERED`
   * `values.batchUpdate`. A cell under a header the tab lacks is dropped.
   */
  suspend fun writeExpenseRepairs(sheetId: String, fixes: List<ExpenseRowFix>): RowWriteResult {
    if (fixes.isEmpty()) return RowWriteResult.Written(null)
    val tab = ExpenseSheetLayout.EXPENSES
    val columns =
      when (val header = headerColumns(sheetId, tab, ExpenseRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    val data =
      fixes.flatMap { fix ->
        mapped(columns, ExpenseRowMapping.cells(fix)).map { (column, value) ->
          ValueRange(range = "$tab!${SheetValues.columnLetter(column)}${fix.row}", values = listOf(listOf(value)))
        }
      }
    if (data.isEmpty()) return RowWriteResult.Written(null)
    return batchUpdate(sheetId, data, written = null)
  }

  /**
   * Deletes the [rows] of [tab] with one `spreadsheets.batchUpdate` of `deleteDimension` requests,
   * highest row first, after looking up the tab's numeric ID with `spreadsheets.get`.
   */
  suspend fun deleteRows(sheetId: String, tab: SheetTab, rows: List<Int>): RowWriteResult {
    if (rows.isEmpty()) return RowWriteResult.Written(null)
    val title = tab.title()
    val tabId =
      when (val r = googleApiCall { sheets.getSpreadsheet(sheetId, TAB_IDS) }) {
        is ApiResult.Failure -> return r.reason.toWriteResult()
        is ApiResult.Success ->
          r.value.sheets.orEmpty().firstOrNull { it.properties?.title.equals(title, ignoreCase = true) }
            ?.properties?.sheetId
      } ?: return RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
    val requests =
      rows.distinct().sortedDescending().map { row ->
        SpreadsheetRequest(
          deleteDimension = DeleteDimensionRequest(DimensionRange(tabId, DimensionRange.ROWS, startIndex = row - 1, endIndex = row))
        )
      }
    return when (val r = googleApiCall { sheets.batchUpdate(sheetId, BatchUpdateSpreadsheetRequest(requests)) }) {
      is ApiResult.Failure -> r.reason.toWriteResult()
      is ApiResult.Success -> RowWriteResult.Written(null)
    }
  }

  /** Appends [dismissals] as rows of the Dismissed tab with one `values.append`, SMS ID and Dismissed On only. */
  suspend fun appendDismissals(sheetId: String, dismissals: List<Dismissal>): RowWriteResult {
    if (dismissals.isEmpty()) return RowWriteResult.Written(null)
    val tab = ExpenseSheetLayout.DISMISSED
    val columns =
      when (val header = headerColumns(sheetId, tab, DismissalRowMapping.requiredHeaders)) {
        is HeaderRead.Failed -> return header.result
        is HeaderRead.Columns -> header.columns
      }
    return appendRows(sheetId, tab, dismissals.map { mapped(columns, DismissalRowMapping.cells(it)) })
  }

  /** Writes [cells] into [row] of [tab] with one `USER_ENTERED` `values.batchUpdate`. */
  private suspend fun updateRow(sheetId: String, tab: String, row: Int, cells: List<Pair<Int, String>>): RowWriteResult =
    batchUpdate(
      sheetId,
      cells.map { (column, value) ->
        ValueRange(range = "$tab!${SheetValues.columnLetter(column)}$row", values = listOf(listOf(value)))
      },
      written = row,
    )

  /** One `USER_ENTERED` `values.batchUpdate` of [data]; [written] is the row a success reports. */
  private suspend fun batchUpdate(sheetId: String, data: List<ValueRange>, written: Int?): RowWriteResult {
    val request = BatchUpdateValuesRequest(valueInputOption = BatchUpdateValuesRequest.USER_ENTERED, data = data)
    return when (val r = googleApiCall { sheets.batchUpdateValues(sheetId, request) }) {
      is ApiResult.Failure -> r.reason.toWriteResult()
      is ApiResult.Success -> RowWriteResult.Written(written)
    }
  }

  private fun SheetTab.title(): String =
    when (this) {
      SheetTab.Expenses -> ExpenseSheetLayout.EXPENSES
      SheetTab.Categories -> ExpenseSheetLayout.CATEGORIES
      SheetTab.Budgets -> ExpenseSheetLayout.BUDGETS
      SheetTab.Dismissed -> ExpenseSheetLayout.DISMISSED
      SheetTab.Settings -> ExpenseSheetLayout.SETTINGS
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
   * invalid are left out, counted as skipped, and their [key] (when it reads) kept in the read's
   * invalid keys and, with the row number, its invalid rows. Expenses keeps both ([readExpenses]).
   */
  private suspend fun <T> readTab(
    sheetId: String,
    tab: String,
    required: List<String>,
    key: (List<Any?>, Map<String, Int>) -> String?,
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
    val invalidKeys = LinkedHashSet<String>()
    val invalidRows = mutableListOf<SheetRow<String>>()
    val rows =
      values.drop(1).mapIndexedNotNull { index, row ->
        if (SheetValues.isBlank(row)) return@mapIndexedNotNull null
        when (val parsed = parse(row, columns)) {
          // values[0] is row 1, so values.drop(1)[index] is row index + 2.
          is ParsedRow.Valid -> SheetRow(row = index + 2, value = parsed.value)
          ParsedRow.Invalid -> {
            skipped++
            key(row, columns)?.let {
              invalidKeys += it
              invalidRows += SheetRow(row = index + 2, value = it)
            }
            null
          }
          ParsedRow.Unkeyed -> null
        }
      }
    return TabReadResult.Rows(rows, skipped, invalidKeys, invalidRows)
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

    /** `spreadsheets.get` fields for each tab's numeric ID and title. */
    const val TAB_IDS = "sheets.properties(sheetId,title)"
  }
}
