package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.google.ExpenseSheetLayout.KEY
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.VALUE
import com.shashi.expensetracker.data.google.SheetValues.cell
import com.shashi.expensetracker.data.google.SheetValues.text
import com.shashi.expensetracker.domain.settings.Setting

/** Turns Settings rows into [Setting]s (AD-7, AD-20). */
internal object SettingRowMapping {

  val requiredHeaders = listOf(KEY, VALUE)

  /** The Setting in [row]; a blank Key is [ParsedRow.Unkeyed]. A blank Value reads as empty. */
  fun setting(row: List<Any?>, columns: Map<String, Int>): ParsedRow<Setting> {
    val key = text(cell(row, columns, KEY)) ?: return ParsedRow.Unkeyed
    return ParsedRow.Valid(Setting(key = key, value = text(cell(row, columns, VALUE)).orEmpty()))
  }
}
