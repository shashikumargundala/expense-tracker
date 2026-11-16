package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.RowWriteResult
import java.util.Locale

/**
 * Adds whatever AD-7's layout is missing, before every sync's reads:
 * 1. Lists the tab titles with `spreadsheets.get` (`fields=sheets.properties.title`).
 * 2. Adds every missing tab with one `spreadsheets.batchUpdate` of `addSheet` requests.
 * 3. Reads row 1 of each existing tab, and writes every missing header (trimmed, case-insensitive)
 *    into that row's first empty cells with one `RAW` `values.batchUpdate`. A new tab gets all its
 *    headers from `A1`. Only empty cells are written, so no existing column moves or changes.
 *
 * Tab titles match case-insensitively, as Sheets does. Nothing is logged here.
 */
internal class SheetLayoutRepair(private val sheets: SheetsService) {

  suspend operator fun invoke(sheetId: String): RowWriteResult {
    val titles =
      when (val r = googleApiCall { sheets.getSpreadsheet(sheetId, TITLES) }) {
        is ApiResult.Failure -> return r.reason.toWriteResult()
        is ApiResult.Success -> r.value.sheets.orEmpty().mapNotNullTo(HashSet()) { it.properties?.title?.fold() }
      }
    val missingTabs = ExpenseSheetLayout.headers.keys.filter { it.fold() !in titles }
    if (missingTabs.isNotEmpty()) {
      val request =
        BatchUpdateSpreadsheetRequest(missingTabs.map { SpreadsheetRequest(addSheet = AddSheetRequest(SheetProperties(title = it))) })
      (googleApiCall { sheets.batchUpdate(sheetId, request) } as? ApiResult.Failure)?.let { return it.reason.toWriteResult() }
    }
    val writes = mutableListOf<ValueRange>()
    for ((tab, headers) in ExpenseSheetLayout.headers) {
      val header =
        if (tab in missingTabs) {
          emptyList()
        } else {
          when (val r = googleApiCall { sheets.getValues(sheetId, "$tab!1:1", ValueRenderOption.UNFORMATTED_VALUE, ValueRenderOption.SERIAL_NUMBER) }) {
            is ApiResult.Failure -> return r.reason.toWriteResult()
            is ApiResult.Success -> r.value.values.firstOrNull().orEmpty()
          }
        }
      writes += headerWrites(tab, header, headers)
    }
    if (writes.isNotEmpty()) {
      val request = BatchUpdateValuesRequest(valueInputOption = BatchUpdateValuesRequest.RAW, data = writes)
      (googleApiCall { sheets.batchUpdateValues(sheetId, request) } as? ApiResult.Failure)?.let {
        return it.reason.toWriteResult()
      }
    }
    return RowWriteResult.Written(null)
  }

  private fun String.fold(): String = lowercase(Locale.ROOT)

  companion object {
    private const val TITLES = "sheets.properties.title"

    /**
     * One cell write per header of [required] that [header] (row 1 of [tab]) lacks, each into the
     * next empty cell of row 1, left to right.
     */
    fun headerWrites(tab: String, header: List<Any?>, required: List<String>): List<ValueRange> {
      val columns = SheetValues.columns(header)
      val missing = required.filter { SheetValues.column(columns, it) == null }
      var index = 0
      return missing.map { name ->
        while (SheetValues.text(header.getOrNull(index)) != null) index++
        val range = "$tab!${SheetValues.columnLetter(index)}1"
        index++
        ValueRange(range = range, values = listOf(listOf(name)))
      }
    }
  }
}
