package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.AccessResult
import java.io.IOException

/**
 * Supplies the `drive.file` access token the Google API client attaches. Implemented by
 * `DriveAuthorization`; tests use a fake. Tokens live in memory only and are never logged.
 */
interface AccessTokenSource {
  /** The current token (cached when possible), or why there is none. Never throws, except cancellation. */
  suspend fun fetchToken(): TokenResult

  /** Drops [token] from the cache and from Google's token cache, so the next fetch gets a fresh one. */
  suspend fun invalidate(token: String)
}

/** A token, or the non-granted [AccessResult] explaining why there is none. */
sealed interface TokenResult {
  class Token(val value: String) : TokenResult {
    override fun equals(other: Any?): Boolean = other is Token && other.value == value

    override fun hashCode(): Int = value.hashCode()

    /** Never prints the token. */
    override fun toString(): String = "Token(<redacted>)"
  }

  /** [access] is [AccessResult.NeedsUserConsent] or [AccessResult.Failure], never `Granted`. */
  data class Denied(val access: AccessResult) : TokenResult
}

/**
 * Thrown inside OkHttp when no token is available, so the request is never sent. [googleApiCall]
 * maps it back to an [ApiFailure].
 */
class GoogleAccessDeniedException(val access: AccessResult) : IOException("Google access not granted")
