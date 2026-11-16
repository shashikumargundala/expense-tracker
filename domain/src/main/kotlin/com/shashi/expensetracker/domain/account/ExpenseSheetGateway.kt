package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting

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
   * Adds whatever AD-7's layout is missing from [sheetId]: each missing tab, with its headers, and
   * each missing header of an existing tab, written into the first empty cells of row 1. Headers
   * match trimmed and case-insensitive. Existing cells are never moved or overwritten. Returns
   * [RowWriteResult.Written] (with a null row) once the layout is complete.
   */
  suspend fun repairLayout(sheetId: String): RowWriteResult

  /**
   * Reads the Expenses tab of [sheetId] fresh. Columns are found by header name, trimmed and
   * case-insensitive, never by position (AD-7). A row with a blank ID is left out and not counted
   * (it's repaired elsewhere); a row with an ID but a missing or unreadable Date, Amount or Category
   * is left out and counted in [TabReadResult.Rows.skipped]. Sync calls [repairLayout] first, so a
   * required header still missing here is an [ExpenseSheetFailure.Unexpected] failure and no mirror
   * is refreshed from the tab. Returns [TabReadResult.SheetMissing] if the Sheet is gone.
   */
  suspend fun readExpenses(sheetId: String): TabReadResult<Expense>

  /** Reads the Categories tab of [sheetId] fresh, as [readExpenses] does. The key is Name. */
  suspend fun readCategories(sheetId: String): TabReadResult<Category>

  /**
   * Reads the Budgets tab of [sheetId] fresh, as [readExpenses] does. A row needs a Month (`yyyy-MM`
   * text or a date), a Category and an Amount above zero; a row with neither Month nor Category is
   * left out uncounted.
   */
  suspend fun readBudgets(sheetId: String): TabReadResult<Budget>

  /** Reads the Dismissed tab of [sheetId] fresh, as [readExpenses] does. The key is SMS ID. */
  suspend fun readDismissals(sheetId: String): TabReadResult<Dismissal>

  /** Reads the Settings tab of [sheetId] fresh, as [readExpenses] does. The key is Key. */
  suspend fun readSettings(sheetId: String): TabReadResult<Setting>

  /**
   * Appends [categories] to the Categories tab of [sheetId], in order, one row each, only in the
   * Name, Color and Icon columns. Used for Categories an Expense or Budget names but the tab lacks
   * (AD-7). Unknown columns are never touched.
   */
  suspend fun appendCategories(sheetId: String, categories: List<Category>): RowWriteResult

  /**
   * Writes [expense] to the Expenses tab of [sheetId], only in the columns the app maps, under the
   * tab's current header. With [existingRow] (the 1-based row holding the Expense's ID in this run's
   * read) it updates that row in place and leaves its Source alone; with null it appends a new row.
   * Unknown columns are never touched.
   */
  suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult
}
