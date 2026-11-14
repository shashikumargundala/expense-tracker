package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense

/**
 * The Expense Sheet in the user's Google Drive. Implemented in `:data`; nothing here exposes a
 * Google type (AD-11). No call throws, except for coroutine cancellation.
 */
interface ExpenseSheetGateway {
  /**
   * Looks for the app's tagged, non-trashed Sheet. The oldest one wins. Returns
   * [ExpenseSheetResult.Ready], [ExpenseSheetResult.NotFound], [ExpenseSheetResult.NeedsConsent] or
   * [ExpenseSheetResult.Failure].
   */
  suspend fun find(): ExpenseSheetResult

  /**
   * Creates the Sheet with its tabs, headers and default Categories, and tags it last so a half-built
   * file is never found. Returns [ExpenseSheetResult.Ready], [ExpenseSheetResult.NeedsConsent] or
   * [ExpenseSheetResult.Failure]; never [ExpenseSheetResult.NotFound].
   */
  suspend fun create(): ExpenseSheetResult

  /**
   * Checks that the Sheet [sheetId] still exists and isn't trashed. Returns
   * [ExpenseSheetResult.Ready] with [sheetId], [ExpenseSheetResult.SheetMissing] when Drive has no
   * such file or it is trashed, [ExpenseSheetResult.NeedsConsent] or [ExpenseSheetResult.Failure];
   * never [ExpenseSheetResult.NotFound].
   */
  suspend fun check(sheetId: String): ExpenseSheetResult

  /**
   * Reads the Expenses tab of [sheetId] fresh. Columns are found by header name, never by position
   * (AD-7); a tab missing a required header is an [ExpenseSheetFailure.Unexpected] failure, so no
   * mirror is refreshed from it. Returns [TabReadResult.SheetMissing] if the Sheet is gone.
   */
  suspend fun readExpenses(sheetId: String): TabReadResult<Expense>

  /** Reads the Categories tab of [sheetId] fresh, as [readExpenses] does. */
  suspend fun readCategories(sheetId: String): TabReadResult<Category>

  /**
   * Writes [expense] to the Expenses tab of [sheetId], only in the columns the app maps, under the
   * tab's current header. With [existingRow] (the 1-based row holding the Expense's ID in this run's
   * read) it updates that row in place and leaves its Source alone; with null it appends a new row.
   * Unknown columns are never touched.
   */
  suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult
}
