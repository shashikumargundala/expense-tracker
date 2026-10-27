package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.AccessResult

/**
 * Hands out `token-1`, `token-2`, … and caches the current one until it is invalidated, like
 * `DriveAuthorization`. Set [denied] to return that result instead of a token.
 */
class FakeTokenSource : AccessTokenSource {
  var denied: AccessResult? = null
  var issued = 0
  val invalidated = mutableListOf<String>()
  private var current: String? = null

  override suspend fun fetchToken(): TokenResult {
    denied?.let { return TokenResult.Denied(it) }
    val token = current ?: "token-${++issued}".also { current = it }
    return TokenResult.Token(token)
  }

  override suspend fun invalidate(token: String) {
    invalidated += token
    if (current == token) current = null
  }
}
