package com.shashi.expensetracker.domain.account

/**
 * Outcome of asking for Google Drive and Sheets access (AD-10). No token appears here: `:data` holds
 * it in memory and attaches it to its own calls.
 */
sealed interface AccessResult {
  /** Access is granted; Drive and Sheets calls can be made. */
  data object Granted : AccessResult

  /**
   * The user must grant access first. [request] is an opaque handle that only the foreground app can
   * launch; background work reports this result and stops.
   */
  data class NeedsUserConsent(val request: ConsentRequest) : AccessResult

  /** From `getAccess()`, [reason] is only [AccessFailure.NotSignedIn] or [AccessFailure.Unavailable]. */
  data class Failure(val reason: AccessFailure) : AccessResult
}

/**
 * Why access could not be obtained. `getAccess()` returns only [NotSignedIn] or [Unavailable];
 * [Unauthorized] comes from a Drive or Sheets call in `:data` whose 401 survived the single retry.
 */
enum class AccessFailure {
  /** No session is stored, so there is no account to authorize. */
  NotSignedIn,

  /**
   * A Drive or Sheets call was rejected (401) even after a fresh token and one retry. Never returned
   * by `getAccess()`.
   */
  Unauthorized,

  /** Authorization is unavailable, for example with no network or no Play services. */
  Unavailable,
}
