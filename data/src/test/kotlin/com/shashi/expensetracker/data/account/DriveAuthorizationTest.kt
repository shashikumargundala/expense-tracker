package com.shashi.expensetracker.data.account

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.google.TokenResult
import com.shashi.expensetracker.domain.account.AccessFailure
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class DriveAuthorizationTest {

  private class FakeSessions(session: Session?) : SessionRepository {
    val session = MutableStateFlow(session)

    override fun observe(): Flow<Session?> = session

    override suspend fun save(session: Session): Boolean = true
  }

  /** Hands out `token-1`, `token-2`, … unless [next] is set; records every call. */
  private class FakeAuthorizer : DriveAuthorization.Authorizer {
    val authorizedEmails = mutableListOf<String>()
    val cleared = mutableListOf<String>()
    var next: (() -> TokenResult)? = null

    override suspend fun authorize(email: String): TokenResult {
      authorizedEmails += email
      return next?.invoke() ?: TokenResult.Token("token-${authorizedEmails.size}")
    }

    override suspend fun clearToken(token: String) {
      cleared += token
    }
  }

  private val sessions = FakeSessions(Session("1234", "shylock@example.com"))
  private val authorizer = FakeAuthorizer()
  private val authorization = DriveAuthorization(sessions, authorizer)

  private fun pendingIntent(): PendingIntent =
    PendingIntent.getActivity(
      ApplicationProvider.getApplicationContext<Context>(),
      0,
      Intent(),
      PendingIntent.FLAG_IMMUTABLE,
    )

  @Test
  fun tokenMapsToToken() {
    assertEquals(TokenResult.Token("abc"), DriveAuthorizationMapping.map(accessToken = "abc", resolution = null))
  }

  @Test
  fun resolutionMapsToNeedsUserConsentWrappingThePendingIntent() {
    val intent = pendingIntent()

    val result = DriveAuthorizationMapping.map(accessToken = null, resolution = intent)

    val access = (result as TokenResult.Denied).access as AccessResult.NeedsUserConsent
    assertSame(intent, (access.request as PendingIntentConsentRequest).pendingIntent)
  }

  @Test
  fun noTokenAndNoResolutionMapsToUnavailable() {
    listOf(null, "", " ").forEach { token ->
      assertEquals(
        TokenResult.Denied(AccessResult.Failure(AccessFailure.Unavailable)),
        DriveAuthorizationMapping.map(accessToken = token, resolution = null),
      )
    }
  }

  @Test
  fun grantedAuthorizesTheSignedInAccount() = runTest {
    assertEquals(AccessResult.Granted, authorization.getAccess())

    assertEquals(listOf("shylock@example.com"), authorizer.authorizedEmails)
  }

  @Test
  fun secondCallInTheSameSessionUsesTheCachedToken() = runTest {
    assertEquals(TokenResult.Token("token-1"), authorization.fetchToken())
    assertEquals(TokenResult.Token("token-1"), authorization.fetchToken())
    assertEquals(AccessResult.Granted, authorization.getAccess())

    assertEquals(1, authorizer.authorizedEmails.size)
  }

  @Test
  fun anotherAccountDoesNotReuseTheCachedToken() = runTest {
    authorization.fetchToken()
    sessions.session.value = Session("5678", "portia@example.com")

    assertEquals(TokenResult.Token("token-2"), authorization.fetchToken())
    assertEquals(listOf("shylock@example.com", "portia@example.com"), authorizer.authorizedEmails)
  }

  @Test
  fun invalidateClearsTheTokenAndTheNextFetchAuthorizesAgain() = runTest {
    authorization.fetchToken()

    authorization.invalidate("token-1")

    assertEquals(listOf("token-1"), authorizer.cleared)
    assertEquals(TokenResult.Token("token-2"), authorization.fetchToken())
  }

  /** Like Google's token cache: returns the old token until its `clearToken` has completed. */
  private class SlowClearAuthorizer : DriveAuthorization.Authorizer {
    val clearGate = CompletableDeferred<Unit>()
    var cleared = false

    override suspend fun authorize(email: String): TokenResult =
      TokenResult.Token(if (cleared) "fresh-token" else "old-token")

    override suspend fun clearToken(token: String) {
      clearGate.await()
      cleared = true
    }
  }

  @Test
  fun fetchRacingAnInvalidateNeverReturnsTheRejectedToken() = runTest {
    val slow = SlowClearAuthorizer()
    val auth = DriveAuthorization(sessions, slow)
    assertEquals(TokenResult.Token("old-token"), auth.fetchToken())

    val invalidation = launch { auth.invalidate("old-token") }
    runCurrent() // invalidate is now suspended inside clearToken
    val racingFetch = async { auth.fetchToken() }
    runCurrent()
    slow.clearGate.complete(Unit)
    invalidation.join()

    assertEquals(TokenResult.Token("fresh-token"), racingFetch.await())
  }

  @Test
  fun invalidatingAStaleTokenKeepsTheCurrentOne() = runTest {
    authorization.fetchToken()

    authorization.invalidate("older-token")

    assertEquals(TokenResult.Token("token-1"), authorization.fetchToken())
    assertEquals(1, authorizer.authorizedEmails.size)
  }

  @Test
  fun signedOutIsNotSignedInWithoutAnAuthorizeCall() = runTest {
    sessions.session.value = null

    assertEquals(AccessResult.Failure(AccessFailure.NotSignedIn), authorization.getAccess())
    assertTrue(authorizer.authorizedEmails.isEmpty())
  }

  @Test
  fun consentNeededIsReturnedAndNotCached() = runTest {
    val consent = AccessResult.NeedsUserConsent(PendingIntentConsentRequest(pendingIntent()))
    authorizer.next = { TokenResult.Denied(consent) }

    assertEquals(consent, authorization.getAccess())
    assertEquals(consent, authorization.getAccess())
    assertEquals(2, authorizer.authorizedEmails.size)
  }

  @Test
  fun authorizeExceptionIsUnavailableAndLogsNoEmail() = runTest {
    ShadowLog.clear()
    authorizer.next = { throw IllegalStateException("shylock@example.com") }

    assertEquals(AccessResult.Failure(AccessFailure.Unavailable), authorization.getAccess())
    assertFalse(ShadowLog.getLogs().any { "shylock@example.com" in it.msg })
  }

  @Test
  fun tokenNeverAppearsInLogsOrToString() = runTest {
    ShadowLog.clear()
    val token = authorization.fetchToken()
    authorization.invalidate("token-1")

    assertFalse("token-1" in token.toString())
    assertFalse(ShadowLog.getLogs().any { "token-1" in it.msg })
  }
}
