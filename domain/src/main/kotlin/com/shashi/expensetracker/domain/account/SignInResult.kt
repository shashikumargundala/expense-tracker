package com.shashi.expensetracker.domain.account

/** Outcome of a sign-in attempt. */
sealed interface SignInResult {
  data class Success(val accountId: String, val email: String) : SignInResult

  /** The user dismissed the account picker. Not a failure. */
  data object Cancelled : SignInResult

  data class Failure(val reason: SignInFailure) : SignInResult
}

/** Why a sign-in failed. */
enum class SignInFailure {
  /** The build has no Web OAuth client ID. */
  NotConfigured,

  /** No screen is in front to show the account picker. */
  NoActivity,

  /** No Google account is available on the device. */
  NoGoogleAccount,

  /** The credential returned could not be read. */
  InvalidCredential,

  /** Google sign-in is unavailable, for example with no network. */
  Unavailable,

  /** The session could not be stored. */
  StorageFailed,
}
