package com.shashi.expensetracker.domain.account

/** The Google account outside the app. Implemented in `:data`. */
interface AccountGateway {
  /** Asks the user to pick a Google account. Maps every outcome to a [SignInResult]. */
  suspend fun signIn(): SignInResult

  /**
   * Gets Drive and Sheets access for the signed-in account without showing any UI. Maps every
   * outcome to an [AccessResult]: [AccessResult.Granted], [AccessResult.NeedsUserConsent], or
   * [AccessResult.Failure] with [AccessFailure.NotSignedIn] or [AccessFailure.Unavailable]. It never
   * returns [AccessFailure.Unauthorized], which only comes from API calls after their single retry.
   */
  suspend fun getAccess(): AccessResult

  /**
   * Shows the consent prompt for [request] in the foreground and waits. Returns true if the user
   * granted access, false if they declined or no screen is in front. Never throws, except for
   * coroutine cancellation.
   */
  suspend fun requestConsent(request: ConsentRequest): Boolean
}
