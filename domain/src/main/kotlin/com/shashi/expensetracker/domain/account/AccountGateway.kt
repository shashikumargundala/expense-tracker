package com.shashi.expensetracker.domain.account

/** The Google account outside the app. Implemented in `:data`. */
interface AccountGateway {
  /** Asks the user to pick a Google account. Maps every outcome to a [SignInResult]. */
  suspend fun signIn(): SignInResult
}
