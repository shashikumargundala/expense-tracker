package com.shashi.expensetracker.account

/**
 * State of [SignInScreen]. [error] is the last failure, cleared on the next attempt. [signedIn]
 * turns true once the session is stored, so the route can open Home.
 */
data class SignInUiState(
  val inProgress: Boolean = false,
  val error: SignInError? = null,
  val signedIn: Boolean = false,
)

/** Which short error line Sign-in shows. */
enum class SignInError {
  /** Network or any other sign-in failure. */
  CouldNotSignIn,

  /** The build has no Web OAuth client ID. */
  NotConfigured,
}
