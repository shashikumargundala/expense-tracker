package com.shashi.expensetracker.domain.account

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Signs in through the [AccountGateway] and, on success, stores the [Session]. Cancel and failure
 * store nothing; a failed save becomes [SignInFailure.StorageFailed]. Never throws to the caller, except for coroutine cancellation.
 */
class SignInUseCase
@Inject
constructor(
  private val gateway: AccountGateway,
  private val sessions: SessionRepository,
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
    val saved = sessions.save(Session(accountId = result.accountId, email = result.email))
    return if (saved) result else SignInResult.Failure(SignInFailure.StorageFailed)
  }
}
