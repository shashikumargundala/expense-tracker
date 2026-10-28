package com.shashi.expensetracker.data.account

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.shashi.expensetracker.data.google.AccessTokenSource
import com.shashi.expensetracker.data.google.TokenResult
import com.shashi.expensetracker.domain.account.AccessFailure
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Drive and Sheets access through `AuthorizationClient` with the `drive.file` scope only, targeted
 * at the signed-in account (AD-10). The token is cached in memory per account, behind a [Mutex], and
 * never stored or logged. No UI is shown: a needed consent comes back as
 * [AccessResult.NeedsUserConsent].
 */
@Singleton
class DriveAuthorization
internal constructor(
  private val sessions: SessionRepository,
  private val authorizer: Authorizer,
) : AccessTokenSource {

  @Inject
  constructor(
    @ApplicationContext context: Context,
    sessions: SessionRepository,
  ) : this(sessions, PlayServicesAuthorizer(context))

  /** The Play services calls, behind an interface so the cache is testable. */
  internal interface Authorizer {
    /** Authorizes `drive.file` for [email]. May throw; [DriveAuthorization] maps exceptions. */
    suspend fun authorize(email: String): TokenResult

    suspend fun clearToken(token: String)
  }

  private val mutex = Mutex()
  private var cachedToken: String? = null
  private var cachedEmail: String? = null

  suspend fun getAccess(): AccessResult =
    when (val result = fetchToken()) {
      is TokenResult.Token -> AccessResult.Granted
      is TokenResult.Denied -> result.access
    }

  override suspend fun fetchToken(): TokenResult =
    mutex.withLock {
      val email =
        sessions.observe().first()?.email?.takeIf { it.isNotBlank() }
          ?: return@withLock denied(AccessFailure.NotSignedIn)
      cachedToken?.let { if (cachedEmail == email) return@withLock TokenResult.Token(it) }
      val result =
        try {
          authorizer.authorize(email)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Authorization error: ${e.javaClass.simpleName}")
          denied(AccessFailure.Unavailable)
        }
      if (result is TokenResult.Token) {
        cachedToken = result.value
        cachedEmail = email
      } else {
        cachedToken = null
        cachedEmail = null
      }
      result
    }

  /**
   * Holds the lock through `clearToken`, so a concurrent [fetchToken] can't authorize before Google
   * has dropped [token] and get the rejected token back.
   */
  override suspend fun invalidate(token: String) {
    mutex.withLock {
      if (cachedToken == token) {
        cachedToken = null
        cachedEmail = null
      }
      try {
        authorizer.clearToken(token)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "clearToken error: ${e.javaClass.simpleName}")
      }
    }
  }

  private fun denied(reason: AccessFailure): TokenResult {
    Log.w(TAG, "Access failed: $reason")
    return TokenResult.Denied(AccessResult.Failure(reason))
  }

  private class PlayServicesAuthorizer(context: Context) : Authorizer {
    private val client = Identity.getAuthorizationClient(context)

    override suspend fun authorize(email: String): TokenResult {
      val request =
        AuthorizationRequest.builder()
          .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
          .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
          .build()
      val result = client.authorize(request).await()
      return DriveAuthorizationMapping.map(
        accessToken = result.accessToken,
        resolution = if (result.hasResolution()) result.pendingIntent else null,
      )
    }

    override suspend fun clearToken(token: String) {
      client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    }
  }

  private companion object {
    const val TAG = "DriveAuthorization"
  }
}

/** The one scope the app ever requests (AD-10). */
internal const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
internal const val GOOGLE_ACCOUNT_TYPE = "com.google"

/** Pure mapping from an `AuthorizationResult`'s fields to a [TokenResult]. */
internal object DriveAuthorizationMapping {
  fun map(accessToken: String?, resolution: PendingIntent?): TokenResult =
    when {
      resolution != null -> TokenResult.Denied(AccessResult.NeedsUserConsent(PendingIntentConsentRequest(resolution)))
      !accessToken.isNullOrBlank() -> TokenResult.Token(accessToken)
      else -> TokenResult.Denied(AccessResult.Failure(AccessFailure.Unavailable))
    }
}

/** The consent prompt `AuthorizationClient` returned. Story 2.3 launches [pendingIntent]. */
class PendingIntentConsentRequest(val pendingIntent: PendingIntent) : ConsentRequest
