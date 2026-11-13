package com.shashi.expensetracker.account

import com.shashi.expensetracker.data.account.AccountModule
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.AccountGateway
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import com.shashi.expensetracker.domain.account.SignInResult
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The session every `@HiltAndroidTest` starts with unless it clears it. */
val SignedInTestSession = Session(accountId = "test-account", email = "tester@example.com", sheetId = "test-sheet")

/** In-memory session store; signed in by default so shell tests launch straight to Home. */
@Singleton
class FakeSessionRepository @Inject constructor() : SessionRepository {
  val session = MutableStateFlow<Session?>(SignedInTestSession)

  override fun observe(): Flow<Session?> = session

  override suspend fun save(session: Session): Boolean {
    this.session.value = session
    return true
  }
}

/** Returns [result] for every sign-in and counts calls; set [access] and [consentGranted] to script consent. */
@Singleton
class FakeAccountGateway @Inject constructor() : AccountGateway {
  var result: SignInResult = SignInResult.Success("test-account", "tester@example.com")
  var calls = 0

  override suspend fun signIn(): SignInResult {
    calls++
    return result
  }

  var access: AccessResult = AccessResult.Granted
  var consentGranted = true
  var consentRequests = 0

  override suspend fun getAccess(): AccessResult = access

  override suspend fun requestConsent(request: ConsentRequest): Boolean {
    consentRequests++
    return consentGranted
  }
}

/** Replaces the real account bindings (Room session, Credential Manager) in every Hilt test. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AccountModule::class])
abstract class FakeAccountModule {
  @Binds abstract fun bindSessionRepository(fake: FakeSessionRepository): SessionRepository

  @Binds abstract fun bindAccountGateway(fake: FakeAccountGateway): AccountGateway

  @Binds abstract fun bindExpenseSheetGateway(fake: FakeExpenseSheetGateway): ExpenseSheetGateway

  companion object {
    @Provides @Singleton fun provideFakeExpenseSheetGateway() = FakeExpenseSheetGateway()
  }
}
