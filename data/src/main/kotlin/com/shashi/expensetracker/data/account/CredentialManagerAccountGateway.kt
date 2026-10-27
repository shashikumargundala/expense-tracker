package com.shashi.expensetracker.data.account

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.shashi.expensetracker.domain.account.AccountGateway
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sign in with Google through Credential Manager (AD-10), with the Web client ID as the server
 * client ID. Shows the picker on the activity [ResumedActivityTracker] holds. Never logs an email
 * or a token.
 */
class CredentialManagerAccountGateway
@Inject
constructor(
  private val activityTracker: ResumedActivityTracker,
  @param:GoogleWebClientId private val webClientId: String,
) : AccountGateway {

  override suspend fun signIn(): SignInResult {
    if (webClientId.isBlank()) return failure(SignInFailure.NotConfigured)
    val activity = activityTracker.current ?: return failure(SignInFailure.NoActivity)
    val request =
      GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
    val result =
      try {
        val response = CredentialManager.create(activity).getCredential(activity, request)
        GoogleCredentialMapping.mapCredential(response.credential)
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialException) {
        Log.w(TAG, "Credential Manager error: ${e.javaClass.simpleName}")
        GoogleCredentialMapping.mapException(e)
      } catch (e: Exception) {
        Log.w(TAG, "Sign-in error: ${e.javaClass.simpleName}")
        SignInResult.Failure(SignInFailure.Unavailable)
      }
    if (result is SignInResult.Failure) Log.w(TAG, "Sign-in failed: ${result.reason}")
    return result
  }

  private fun failure(reason: SignInFailure): SignInResult {
    Log.w(TAG, "Sign-in failed: $reason")
    return SignInResult.Failure(reason)
  }

  private companion object {
    const val TAG = "AccountGateway"
  }
}
