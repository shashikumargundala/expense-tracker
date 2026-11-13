package com.shashi.expensetracker.data.account

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.AccountGateway
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Sign in with Google through Credential Manager (AD-10), with the Web client ID as the server
 * client ID. Shows the picker on the activity [ResumedActivityTracker] holds. Drive access is
 * delegated to [DriveAuthorization]. Never logs an email or a token.
 */
class CredentialManagerAccountGateway
@Inject
constructor(
  private val activityTracker: ResumedActivityTracker,
  @param:GoogleWebClientId private val webClientId: String,
  private val driveAuthorization: DriveAuthorization,
) : AccountGateway {

  override suspend fun getAccess(): AccessResult = driveAuthorization.getAccess()

  /**
   * Launches the consent `PendingIntent` on the tracked activity and waits for its result. The
   * activity must be a [ComponentActivity] in front; otherwise this returns false.
   */
  override suspend fun requestConsent(request: ConsentRequest): Boolean {
    val pending = (request as? PendingIntentConsentRequest)?.pendingIntent ?: return false
    val activity = activityTracker.current as? ComponentActivity ?: return false
    return withContext(Dispatchers.Main.immediate) {
      var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
      var observer: LifecycleEventObserver? = null
      try {
        suspendCancellableCoroutine { cont ->
          // A destroyed activity (rotation, theme change) never delivers a result: end as a decline.
          val destroyObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY && cont.isActive) cont.resume(false)
          }
          observer = destroyObserver
          activity.lifecycle.addObserver(destroyObserver)
          val registered =
            activity.activityResultRegistry.register(
              "drive-consent-${UUID.randomUUID()}",
              ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
              if (cont.isActive) cont.resume(result.resultCode == Activity.RESULT_OK)
            }
          launcher = registered
          try {
            registered.launch(IntentSenderRequest.Builder(pending).build())
          } catch (e: Exception) {
            Log.w(TAG, "Consent launch error: ${e.javaClass.simpleName}")
            if (cont.isActive) cont.resume(false)
          }
        }
      } finally {
        launcher?.unregister()
        observer?.let { activity.lifecycle.removeObserver(it) }
      }
    }
  }

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
    if (result is SignInResult.Success) driveAuthorization.useAccount(result.email)
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
