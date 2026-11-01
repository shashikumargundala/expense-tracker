package com.shashi.expensetracker.domain.account

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Signs in through the [AccountGateway], then runs [EnsureExpenseSheetUseCase], which stores the
 * [Session] once the Expense Sheet ID is known. Cancel, a declined consent prompt and failure store
 * nothing; the declined prompt is reported as [SignInResult.Cancelled]. A failed save becomes
 * [SignInFailure.StorageFailed]. Never throws to the caller, except for coroutine cancellation.
 */
class SignInUseCase
@Inject
constructor(
  private val gateway: AccountGateway,
  private val ensureSheet: EnsureExpenseSheetUseCase,
) {
  suspend operator fun invoke(): SignInResult {
    val result =
      try {
        gateway.signIn()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        return SignInResult.Failure(SignInFailure.Unavailable)
      }
    if (result !is SignInResult.Success) return result
    return when (val sheet = ensureSheet(result.accountId, result.email)) {
      is EnsureExpenseSheetResult.Ready -> result
      EnsureExpenseSheetResult.ConsentDeclined -> SignInResult.Cancelled
      is EnsureExpenseSheetResult.Failure ->
        SignInResult.Failure(
          if (sheet.reason == ExpenseSheetFailure.StorageFailed) SignInFailure.StorageFailed
          else SignInFailure.Unavailable
        )
    }
  }
}
