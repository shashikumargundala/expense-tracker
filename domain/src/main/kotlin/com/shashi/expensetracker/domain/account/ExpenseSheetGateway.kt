package com.shashi.expensetracker.domain.account

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
}
