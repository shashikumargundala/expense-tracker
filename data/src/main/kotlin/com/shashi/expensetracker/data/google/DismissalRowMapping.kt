package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.google.ExpenseSheetLayout.DISMISSED_ON
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.SMS_ID
import com.shashi.expensetracker.data.google.SheetValues.asText
import com.shashi.expensetracker.data.google.SheetValues.cell
import com.shashi.expensetracker.data.google.SheetValues.text
import com.shashi.expensetracker.domain.inbox.Dismissal

/** Turns Dismissed rows into [Dismissal]s and back (AD-7, AD-13). */
internal object DismissalRowMapping {

  val requiredHeaders = listOf(SMS_ID, DISMISSED_ON)

  /** The Dismissal in [row]; a blank SMS ID is [ParsedRow.Unkeyed], an unreadable date [ParsedRow.Invalid]. */
  fun dismissal(row: List<Any?>, columns: Map<String, Int>): ParsedRow<Dismissal> {
    val smsId = text(cell(row, columns, SMS_ID)) ?: return ParsedRow.Unkeyed
    val on = SheetValues.date(cell(row, columns, DISMISSED_ON)) ?: return ParsedRow.Invalid
    return ParsedRow.Valid(Dismissal(smsId = smsId, dismissedOn = on))
  }

  /** The cells to append for [dismissal], header name to `USER_ENTERED` value. */
  fun cells(dismissal: Dismissal): Map<String, String> =
    mapOf(SMS_ID to asText(dismissal.smsId), DISMISSED_ON to SheetValues.formatDate(dismissal.dismissedOn))
}
