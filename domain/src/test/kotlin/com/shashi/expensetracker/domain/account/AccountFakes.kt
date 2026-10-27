package com.shashi.expensetracker.domain.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository(initial: Session? = null) : SessionRepository {
  val session = MutableStateFlow(initial)
  var failSave = false
  var saveCount = 0

  override fun observe(): Flow<Session?> = session

  override suspend fun save(session: Session): Boolean {
    if (failSave) return false
    saveCount++
    this.session.value = session
    return true
  }
}

class FakeAccountGateway(private val signIn: () -> SignInResult) : AccountGateway {
  var access: AccessResult = AccessResult.Granted

  override suspend fun signIn(): SignInResult = signIn.invoke()

  override suspend fun getAccess(): AccessResult = access
}
