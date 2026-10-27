package com.shashi.expensetracker.data.google

import com.squareup.moshi.JsonClass

/** A Sheets v4 `Spreadsheet`, with only the fields used to create one and read back its ID. */
@JsonClass(generateAdapter = true)
data class Spreadsheet(
  val spreadsheetId: String? = null,
  val properties: SpreadsheetProperties? = null,
  val sheets: List<Sheet>? = null,
)

@JsonClass(generateAdapter = true) data class SpreadsheetProperties(val title: String? = null)

/** One tab of a [Spreadsheet]. */
@JsonClass(generateAdapter = true) data class Sheet(val properties: SheetProperties? = null)

@JsonClass(generateAdapter = true)
data class SheetProperties(val sheetId: Int? = null, val title: String? = null)

/**
 * A Sheets v4 `ValueRange`: [range] in A1 notation (`Expenses!A1:G1`) and its [values] row by row.
 * Cells are strings, numbers or booleans.
 */
@JsonClass(generateAdapter = true)
data class ValueRange(
  val range: String,
  val values: List<List<Any?>> = emptyList(),
  val majorDimension: String? = null,
)

/** The body of `spreadsheets.values.batchUpdate`. */
@JsonClass(generateAdapter = true)
data class BatchUpdateValuesRequest(val valueInputOption: String, val data: List<ValueRange>) {
  companion object {
    /** Values are parsed as if typed into the Sheet; AD-7 requires it for `Date`. */
    const val USER_ENTERED = "USER_ENTERED"
    const val RAW = "RAW"
  }
}
