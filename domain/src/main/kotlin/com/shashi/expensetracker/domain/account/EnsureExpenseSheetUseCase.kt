package com.shashi.expensetracker.domain.account

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of [EnsureExpenseSheetUseCase]. */
sealed interface EnsureExpenseSheetResult {
  /** The Sheet is known and the session, with its ID, is stored. */
  data class Ready(val sheetId: String) : EnsureExpenseSheetResult

  /** The user declined the consent prompt. Nothing is stored. */
  data object ConsentDeclined : EnsureExpenseSheetResult

  data class Failure(val reason: ExpenseSheetFailure) : EnsureExpenseSheetResult
}

/**
 * After Google sign-in, or when [RecoverExpenseSheetUseCase] finds the stored Sheet missing during a
 * session: gets Drive access (showing the consent prompt if needed), finds the Expense Sheet or
 * creates it, then stores the whole [Session] once, with its `sheetId`. A failure or a declined
 * prompt stores nothing. Never throws, except for coroutine cancellation.
 */
class EnsureExpenseSheetUseCase
@Inject
constructor(
  private val accounts: AccountGateway,
  private val sheets: ExpenseSheetGateway,
  private val sessions: SessionRepository,
) {
  suspend operator fun invoke(accountId: String, email: String): EnsureExpenseSheetResult =
    try {
      run(accountId, email)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected)
    }

  private suspend fun run(accountId: String, email: String): EnsureExpenseSheetResult {
    when (val access = accounts.getAccess()) {
      AccessResult.Granted -> Unit
      is AccessResult.NeedsUserConsent ->
        if (!accounts.requestConsent(access.request)) return EnsureExpenseSheetResult.ConsentDeclined
      is AccessResult.Failure -> return EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access)
    }
    val sheetId =
      when (val found = withConsent { sheets.find() }) {
        is ExpenseSheetResult.Ready -> found.sheetId
        ExpenseSheetResult.NotFound ->
          when (val created = withConsent { sheets.create() }) {
            is ExpenseSheetResult.Ready -> created.sheetId
            else -> return created.toOutcome()
          }
        else -> return found.toOutcome()
      }
    val saved = sessions.save(Session(accountId = accountId, email = email, sheetId = sheetId))
    return if (saved) {
      EnsureExpenseSheetResult.Ready(sheetId)
    } else {
      EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.StorageFailed)
    }
  }

  /**
   * Runs [call]; if it needs consent (for example a revoked grant), asks once and retries once.
   * Returns [ExpenseSheetResult.NeedsConsent] only when the user declined; if the retry still needs
   * consent after a granted prompt, that is an [ExpenseSheetFailure.Access] failure.
   */
  internal suspend fun withConsent(call: suspend () -> ExpenseSheetResult): ExpenseSheetResult {
    val first = call()
    if (first !is ExpenseSheetResult.NeedsConsent) return first
    if (!accounts.requestConsent(first.request)) return first
    val retry = call()
    return if (retry is ExpenseSheetResult.NeedsConsent) {
      ExpenseSheetResult.Failure(ExpenseSheetFailure.Access)
    } else {
      retry
    }
  }

  /** Maps a non-success gateway result (or a [ExpenseSheetResult.Ready]) to this use case's result. */
  internal fun ExpenseSheetResult.toOutcome(): EnsureExpenseSheetResult =
    when (this) {
      is ExpenseSheetResult.Failure -> EnsureExpenseSheetResult.Failure(reason)
      is ExpenseSheetResult.NeedsConsent -> EnsureExpenseSheetResult.ConsentDeclined
      is ExpenseSheetResult.Ready -> EnsureExpenseSheetResult.Ready(sheetId)
      // Callers handle NotFound and SheetMissing before mapping; find() and create() never return
      // SheetMissing, and check() never returns NotFound.
      ExpenseSheetResult.NotFound,
      ExpenseSheetResult.SheetMissing -> EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected)
    }
}
