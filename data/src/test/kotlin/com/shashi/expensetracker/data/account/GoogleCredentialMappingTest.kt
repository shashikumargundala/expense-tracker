package com.shashi.expensetracker.data.account

import android.os.Bundle
import android.util.Base64
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleCredentialMappingTest {

  private fun jwt(payloadJson: String): String {
    fun part(s: String) =
      Base64.encodeToString(s.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    return "${part("""{"alg":"RS256"}""")}.${part(payloadJson)}.signature"
  }

  @Test
  fun idTokenSubAndEmailBecomeSuccess() {
    val token = jwt("""{"sub":"1098765","email":"shylock@example.com","name":"Shylock"}""")

    assertEquals(
      SignInResult.Success(accountId = "1098765", email = "shylock@example.com"),
      GoogleCredentialMapping.mapIdToken(token, fallbackEmail = "other@example.com"),
    )
  }

  @Test
  fun missingEmailClaimFallsBackToCredentialId() {
    val token = jwt("""{"sub":"1098765"}""")

    assertEquals(
      SignInResult.Success("1098765", "shylock@example.com"),
      GoogleCredentialMapping.mapIdToken(token, fallbackEmail = "shylock@example.com"),
    )
  }

  @Test
  fun missingSubIsInvalidCredential() {
    val token = jwt("""{"email":"shylock@example.com"}""")

    assertEquals(
      SignInResult.Failure(SignInFailure.InvalidCredential),
      GoogleCredentialMapping.mapIdToken(token, fallbackEmail = "shylock@example.com"),
    )
  }

  @Test
  fun malformedTokenIsInvalidCredential() {
    listOf("not-a-jwt", "a.%%%.c", "a.${jwt("x").split('.')[1]}.c").forEach { token ->
      assertEquals(
        token,
        SignInResult.Failure(SignInFailure.InvalidCredential),
        GoogleCredentialMapping.mapIdToken(token, fallbackEmail = "shylock@example.com"),
      )
    }
  }

  @Test
  fun googleIdTokenCredentialIsMappedFromItsToken() {
    val credential =
      GoogleIdTokenCredential.Builder()
        .setId("shylock@example.com")
        .setIdToken(jwt("""{"sub":"1098765","email":"shylock@example.com"}"""))
        .build()

    assertEquals(SignInResult.Success("1098765", "shylock@example.com"), GoogleCredentialMapping.mapCredential(credential))
  }

  @Test
  fun otherCredentialTypeIsInvalidCredential() {
    assertEquals(
      SignInResult.Failure(SignInFailure.InvalidCredential),
      GoogleCredentialMapping.mapCredential(CustomCredential("com.example.OTHER", Bundle())),
    )
  }

  @Test
  fun cancellationIsCancelledNotFailure() {
    assertEquals(SignInResult.Cancelled, GoogleCredentialMapping.mapException(GetCredentialCancellationException()))
  }

  @Test
  fun noCredentialIsNoGoogleAccount() {
    assertEquals(
      SignInResult.Failure(SignInFailure.NoGoogleAccount),
      GoogleCredentialMapping.mapException(NoCredentialException()),
    )
  }

  @Test
  fun otherErrorsAreUnavailable() {
    listOf(GetCredentialUnknownException(), GetCredentialInterruptedException()).forEach { e ->
      assertEquals(SignInResult.Failure(SignInFailure.Unavailable), GoogleCredentialMapping.mapException(e))
    }
  }

  /** Drive access isn't used by sign-in, so the dependency never authorizes. */
  private val unusedDriveAuthorization =
    DriveAuthorization(
      sessions =
        object : SessionRepository {
          override fun observe() = flowOf<Session?>(null)

          override suspend fun save(session: Session) = false
        },
      authorizer =
        object : DriveAuthorization.Authorizer {
          override suspend fun authorize(email: String) = error("unused")

          override suspend fun clearToken(token: String) = error("unused")
        },
    )

  @Test
  fun blankWebClientIdFailsAsNotConfiguredWithoutTouchingCredentialManager() = runTest {
    val gateway = CredentialManagerAccountGateway(ResumedActivityTracker(), webClientId = " ", unusedDriveAuthorization)

    assertEquals(SignInResult.Failure(SignInFailure.NotConfigured), gateway.signIn())
  }

  @Test
  fun noResumedActivityFailsAsNoActivity() = runTest {
    val gateway = CredentialManagerAccountGateway(ResumedActivityTracker(), webClientId = "web.apps.googleusercontent.com", unusedDriveAuthorization)

    assertEquals(SignInResult.Failure(SignInFailure.NoActivity), gateway.signIn())
  }
}
