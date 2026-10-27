package com.shashi.expensetracker.data.google

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Answers a 401 by invalidating the rejected token (`clearToken`), fetching a fresh one and
 * retrying once (AD-10). OkHttp calls this again for every 401, so a response that already
 * followed a retry (`priorResponse != null`) gets null and the 401 surfaces: no loop.
 */
class TokenAuthenticator @Inject constructor(private val tokens: AccessTokenSource) : Authenticator {

  override fun authenticate(route: Route?, response: Response): Request? {
    if (response.priorResponse != null) return null
    val rejected =
      response.request.header(AUTHORIZATION)?.removePrefix(BEARER)?.takeIf { it.isNotEmpty() }
        ?: return null
    return runBlocking {
      tokens.invalidate(rejected)
      when (val result = tokens.fetchToken()) {
        is TokenResult.Token -> response.request.withBearer(result.value)
        is TokenResult.Denied -> throw GoogleAccessDeniedException(result.access)
      }
    }
  }
}

internal const val AUTHORIZATION = "Authorization"
internal const val BEARER = "Bearer "

internal fun Request.withBearer(token: String): Request =
  newBuilder().header(AUTHORIZATION, BEARER + token).build()
