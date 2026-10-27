package com.shashi.expensetracker.data.account

import android.util.Base64
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import org.json.JSONException
import org.json.JSONObject

/**
 * Pure mapping from Credential Manager outcomes to [SignInResult], kept apart from the
 * [CredentialManagerAccountGateway] so it is testable without Credential Manager.
 */
internal object GoogleCredentialMapping {

  /** Maps the returned credential: a Google ID token becomes [SignInResult.Success]. */
  fun mapCredential(credential: Credential): SignInResult {
    if (
      credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      return SignInResult.Failure(SignInFailure.InvalidCredential)
    }
    val google =
      try {
        GoogleIdTokenCredential.createFrom(credential.data)
      } catch (e: GoogleIdTokenParsingException) {
        return SignInResult.Failure(SignInFailure.InvalidCredential)
      }
    return mapIdToken(google.idToken, fallbackEmail = google.id)
  }

  /**
   * Reads `sub` and `email` from the ID token's payload. The token comes straight from the
   * platform and no server trusts it, so the signature isn't checked. The token is never stored.
   */
  fun mapIdToken(idToken: String, fallbackEmail: String): SignInResult {
    val payload = decodePayload(idToken) ?: return SignInResult.Failure(SignInFailure.InvalidCredential)
    val sub = payload.optString("sub").trim()
    val email = payload.optString("email").trim().ifEmpty { fallbackEmail.trim() }
    if (sub.isEmpty() || email.isEmpty()) {
      return SignInResult.Failure(SignInFailure.InvalidCredential)
    }
    return SignInResult.Success(accountId = sub, email = email)
  }

  /** Cancel is not a failure; everything else is a named [SignInFailure]. */
  fun mapException(e: GetCredentialException): SignInResult =
    when (e) {
      is GetCredentialCancellationException -> SignInResult.Cancelled
      is NoCredentialException -> SignInResult.Failure(SignInFailure.NoGoogleAccount)
      else -> SignInResult.Failure(SignInFailure.Unavailable)
    }

  private fun decodePayload(jwt: String): JSONObject? {
    val parts = jwt.split('.')
    if (parts.size != 3) return null
    return try {
      val bytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
      JSONObject(String(bytes, Charsets.UTF_8))
    } catch (e: IllegalArgumentException) {
      null
    } catch (e: JSONException) {
      null
    }
  }
}
