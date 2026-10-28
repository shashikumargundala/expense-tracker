package com.shashi.expensetracker.data.google

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

/** Each model parses a recorded Drive or Sheets response, ignoring fields it doesn't declare. */
class GoogleApiModelsTest {

  private val moshi = Moshi.Builder().build()

  private fun sample(name: String): String =
    checkNotNull(javaClass.getResource("/google/$name")) { "Missing sample $name" }.readText()

  private inline fun <reified T> parse(name: String): T =
    checkNotNull(moshi.adapter(T::class.java).fromJson(sample(name)))

  @Test
  fun fileListParsesFilesWithAppProperties() {
    val list = parse<FileList>("drive-files-list.json")

    assertEquals(2, list.files.size)
    assertEquals(
      DriveFile(
        id = "1AbCdEfGhIjKlMnOpQrStUvWxYz0123456789",
        name = "Expense Tracker",
        mimeType = "application/vnd.google-apps.spreadsheet",
        createdTime = "2026-10-28T09:15:00.000Z",
        appProperties = mapOf("expenseTracker" to "v1"),
      ),
      list.files[0],
    )
    assertEquals(null, list.files[1].appProperties)
  }

  @Test
  fun emptyFileListHasNoFiles() {
    assertEquals(FileList(), moshi.adapter(FileList::class.java).fromJson("""{"kind":"drive#fileList"}"""))
  }

  @Test
  fun updatedFileParses() {
    val file = parse<DriveFile>("drive-file-update.json")

    assertEquals("1AbCdEfGhIjKlMnOpQrStUvWxYz0123456789", file.id)
    assertEquals(mapOf("expenseTracker" to "v1"), file.appProperties)
  }

  @Test
  fun createdSpreadsheetParsesIdTitleAndTabs() {
    val spreadsheet = parse<Spreadsheet>("sheets-spreadsheet-create.json")

    assertEquals("1AbCdEfGhIjKlMnOpQrStUvWxYz0123456789", spreadsheet.spreadsheetId)
    assertEquals(SpreadsheetProperties(title = "Expense Tracker"), spreadsheet.properties)
    assertEquals(
      listOf(SheetProperties(sheetId = 0, title = "Expenses"), SheetProperties(sheetId = 1849264512, title = "Categories")),
      spreadsheet.sheets?.map { it.properties },
    )
  }

  @Test
  fun valueRangeParsesMixedCells() {
    val range = parse<ValueRange>("sheets-value-range.json")

    assertEquals("Expenses!A1:G2", range.range)
    assertEquals("ROWS", range.majorDimension)
    assertEquals(listOf("ID", "Date", "Amount", "Category", "Description", "Source", "SMS ID"), range.values[0])
    assertEquals(listOf("0f8fad5b-d9cb-469f-a165-70867728950e", 46323.5, 250.75, "Food", "Lunch", "User", ""), range.values[1])
  }

  @Test
  fun batchUpdateRequestSerializesWithoutNulls() {
    val request =
      BatchUpdateValuesRequest(
        valueInputOption = BatchUpdateValuesRequest.USER_ENTERED,
        data = listOf(ValueRange(range = "Budgets!A2:C2", values = listOf(listOf("2026-10", "Food", 5000)))),
      )

    assertEquals(
      """{"valueInputOption":"USER_ENTERED","data":[{"range":"Budgets!A2:C2","values":[["2026-10","Food",5000]]}]}""",
      moshi.adapter(BatchUpdateValuesRequest::class.java).toJson(request),
    )
  }
}
