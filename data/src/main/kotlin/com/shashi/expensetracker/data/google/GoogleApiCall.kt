package com.shashi.expensetracker.data.google

import android.util.Log
import com.shashi.expensetracker.domain.account.AccessFailure
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

/** Outcome of one Drive or Sheets call, with every exception mapped (Errors convention). */
sealed interface ApiResult<out T> {
  data class Success<T>(val value: T) : ApiResult<T>

  data class Failure(val reason: ApiFailure) : ApiResult<Nothing>
}

/** Why a Drive or Sheets call failed. */
sealed interface ApiFailure {
  /** The user must consent first; nothing was sent. */
  data class NeedsUserConsent(val request: ConsentRequest) : ApiFailure

  /** No token could be had ([AccessFailure.NotSignedIn], [AccessFailure.Unavailable]), or a 401 survived the one retry ([AccessFailure.Unauthorized]). */
  data class Access(val reason: AccessFailure) : ApiFailure

  /** No response: offline, timeout or a dropped connection. */
  data object Network : ApiFailure

  /** Any other non-2xx response. */
  data class Http(val code: Int) : ApiFailure

  /** A response that couldn't be read, or any other unexpected error. */
  data object Unexpected : ApiFailure
}

/**
 * Runs one Retrofit call and maps every outcome to an [ApiResult]. Rethrows cancellation. Logs only
 * the failure kind, never a URL, header, body or token.
 */
suspend fun <T> googleApiCall(call: suspend () -> T): ApiResult<T> {
  val failure =
    try {
      return ApiResult.Success(call())
    } catch (e: CancellationException) {
      throw e
    } catch (e: GoogleAccessDeniedException) {
      when (val access = e.access) {
        is AccessResult.NeedsUserConsent -> ApiFailure.NeedsUserConsent(access.request)
        is AccessResult.Failure -> ApiFailure.Access(access.reason)
        AccessResult.Granted -> ApiFailure.Unexpected
      }
    } catch (e: HttpException) {
      if (e.code() == 401) ApiFailure.Access(AccessFailure.Unauthorized) else ApiFailure.Http(e.code())
    } catch (e: JsonEncodingException) {
      ApiFailure.Unexpected
    } catch (e: JsonDataException) {
      ApiFailure.Unexpected
    } catch (e: IOException) {
      ApiFailure.Network
    } catch (e: Exception) {
      ApiFailure.Unexpected
    }
  Log.w(TAG, "Google API call failed: ${failure.javaClass.simpleName}")
  return ApiResult.Failure(failure)
}

private const val TAG = "GoogleApi"
