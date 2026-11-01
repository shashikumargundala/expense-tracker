package com.shashi.expensetracker.data.google

/** The Expense Sheet's tabs, headers and default Categories (AD-7). */
internal object ExpenseSheetLayout {
  const val TITLE = "Expense Tracker"

  /** The Drive `appProperties` marker that makes the file findable; set last on create. */
  const val MARKER_KEY = "expenseTracker"
  const val MARKER_VALUE = "v1"

  const val EXPENSES = "Expenses"
  const val CATEGORIES = "Categories"
  const val BUDGETS = "Budgets"
  const val DISMISSED = "Dismissed"
  const val SETTINGS = "Settings"

  /** Tab title to its header row, in tab order. */
  val headers: Map<String, List<String>> =
    linkedMapOf(
      EXPENSES to listOf("ID", "Date", "Amount", "Category", "Description", "Source", "SMS ID"),
      CATEGORIES to listOf("Name", "Color", "Icon"),
      BUDGETS to listOf("Month", "Category", "Amount"),
      DISMISSED to listOf("SMS ID", "Dismissed On"),
      SETTINGS to listOf("Key", "Value"),
    )

  /** Name, colour and icon of the five default Categories. */
  val defaultCategories: List<List<String>> =
    listOf(
      listOf("Food", "#F59E0B", "restaurant"),
      listOf("Transport", "#10B981", "directions_car"),
      listOf("Shopping", "#E11D48", "shopping_bag"),
      listOf("Bills", "#3B82F6", "receipt_long"),
      listOf("Fun", "#A855F7", "celebration"),
    )

  /** The `files.list` query for the tagged, non-trashed Sheet. */
  const val FIND_QUERY =
    "appProperties has { key='$MARKER_KEY' and value='$MARKER_VALUE' } and trashed=false"

  fun newSpreadsheet(): Spreadsheet =
    Spreadsheet(
      properties = SpreadsheetProperties(title = TITLE),
      sheets = headers.keys.map { Sheet(SheetProperties(title = it)) },
    )

  /** Headers for every tab, plus the Categories rows under their header. */
  fun initialValues(): BatchUpdateValuesRequest =
    BatchUpdateValuesRequest(
      valueInputOption = BatchUpdateValuesRequest.USER_ENTERED,
      data =
        headers.map { (tab, header) ->
          val rows = if (tab == CATEGORIES) listOf(header) + defaultCategories else listOf(header)
          ValueRange(range = "$tab!A1", values = rows)
        },
    )
}
