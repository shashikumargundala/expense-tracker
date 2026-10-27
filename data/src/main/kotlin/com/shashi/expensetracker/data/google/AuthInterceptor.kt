package com.shashi.expensetracker.data.google

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to every Google API request. With no token, it throws
 * [GoogleAccessDeniedException] and nothing is sent. Never logs headers or bodies.
 */
class AuthInterceptor @Inject constructor(private val tokens: AccessTokenSource) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    // OkHttp calls interceptors on its own threads, so blocking here doesn't block a coroutine.
    val token =
      when (val result = runBlocking { tokens.fetchToken() }) {
        is TokenResult.Token -> result.value
        is TokenResult.Denied -> throw GoogleAccessDeniedException(result.access)
      }
    return chain.proceed(chain.request().withBearer(token))
  }
}
