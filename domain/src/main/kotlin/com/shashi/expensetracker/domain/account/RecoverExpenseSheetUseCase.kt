package com.shashi.expensetracker.domain.account

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Missing-Sheet recovery (AD-20), called by sync. Checks the stored session's Sheet; if it was
 * deleted or trashed, runs [EnsureExpenseSheetUseCase] for the stored account, which finds another
 * tagged Sheet or creates one and stores its ID. With no stored session or Sheet ID it is an
 * [ExpenseSheetFailure.Access] failure and nothing is called. A failed check changes nothing. The
 * consent rule and the result mapping are [EnsureExpenseSheetUseCase]'s own, so the two never drift.
 * With no screen in front, a needed consent prompt can't show, so the result is
 * [EnsureExpenseSheetResult.ConsentDeclined]. Never throws, except for coroutine cancellation.
 */
class RecoverExpenseSheetUseCase
@Inject
constructor(
  private val sheets: ExpenseSheetGateway,
  private val sessions: SessionRepository,
  private val ensureExpenseSheet: EnsureExpenseSheetUseCase,
) {
  suspend operator fun invoke(): EnsureExpenseSheetResult =
    try {
      run()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected)
    }

  private suspend fun run(): EnsureExpenseSheetResult {
    val session = sessions.observe().first()
    val sheetId = session?.sheetId ?: return EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access)
    return with(ensureExpenseSheet) {
      when (val checked = withConsent { sheets.check(sheetId) }) {
        ExpenseSheetResult.SheetMissing -> ensureExpenseSheet(session.accountId, session.email)
        else -> checked.toOutcome()
      }
    }
  }
}
