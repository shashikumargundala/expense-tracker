package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.google.ExpenseSheetLayout.AMOUNT
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.CATEGORY
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.DATE
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.DESCRIPTION
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.ID
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.SMS_ID
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.SOURCE
import com.shashi.expensetracker.data.google.SheetValues.asText
import com.shashi.expensetracker.data.google.SheetValues.cell
import com.shashi.expensetracker.data.google.SheetValues.text
import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource

/** Turns Expenses rows into [Expense]s and back (AD-7), with [SheetValues]' parsing. */
internal object ExpenseRowMapping {

  /** The Expenses headers a row needs to be read; Description, Source and SMS ID may be absent. */
  val requiredHeaders = listOf(ID, DATE, AMOUNT, CATEGORY)

  /**
   * [row] as an [ExpenseRowRead]. A blank ID is a null `id`, and the rest is still parsed so a
   * hand-typed row can be repaired (AD-5). A missing or unreadable Date, Amount or Category makes it
   * invalid (a null `expense`). A blank or unknown Source reads as User (AD-7), and a blank one is
   * flagged so the repair can write `User` back.
   */
  fun row(row: List<Any?>, columns: Map<String, Int>): ExpenseRowRead {
    fun cell(name: String): Any? = cell(row, columns, name)
    val id = text(cell(ID))
    val source = text(cell(SOURCE))
    return ExpenseRowRead(id = id, expense = expense(id.orEmpty(), source, ::cell), sourceBlank = source == null)
  }

  private fun expense(id: String, source: String?, cell: (String) -> Any?): Expense? {
    val date = SheetValues.date(cell(DATE)) ?: return null
    val amount = SheetValues.amountPaise(cell(AMOUNT)) ?: return null
    val category = text(cell(CATEGORY)) ?: return null
    return Expense(
      id = id,
      date = date,
      amountPaise = amount,
      category = category,
      description = text(cell(DESCRIPTION)).orEmpty(),
      source = if (source.equals("App", ignoreCase = true)) ExpenseSource.App else ExpenseSource.User,
      smsId = text(cell(SMS_ID)),
    )
  }

  /**
   * The cells AD-5's [fix] writes, header name to `USER_ENTERED` value: only ID, Source and SMS ID,
   * and only those the fix names.
   */
  fun cells(fix: ExpenseRowFix): Map<String, String> =
    buildMap {
      fix.id?.let { put(ID, asText(it)) }
      if (fix.sourceUser) put(SOURCE, ExpenseSource.User.name)
      if (fix.clearSmsId) put(SMS_ID, "")
    }

  /**
   * The cells to write for [expense], header name to `USER_ENTERED` value. Source is included only
   * for a new row ([withSource]): app edits never change an existing Source (AD-7). Text is written
   * with [SheetValues.asText] so the Sheet keeps it as typed.
   */
  fun cells(expense: Expense, withSource: Boolean): Map<String, String> =
    buildMap {
      put(ID, asText(expense.id))
      put(DATE, SheetValues.formatDate(expense.date))
      put(AMOUNT, SheetValues.formatAmount(expense.amountPaise))
      put(CATEGORY, asText(expense.category))
      put(DESCRIPTION, asText(expense.description))
      if (withSource) put(SOURCE, expense.source.name)
      put(SMS_ID, asText(expense.smsId.orEmpty()))
    }
}
