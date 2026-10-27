package com.shashi.expensetracker.domain.account

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveSessionUseCaseTest {

  @Test
  fun `emits null when signed out`() = runTest {
    assertNull(ObserveSessionUseCase(FakeSessionRepository())().first())
  }

  @Test
  fun `emits the stored session`() = runTest {
    val session = Session("1234", "shylock@example.com")

    assertEquals(session, ObserveSessionUseCase(FakeSessionRepository(session))().first())
  }

  @Test
  fun `emits again after sign-in saves a session`() = runTest {
    val sessions = FakeSessionRepository()
    val emissions = mutableListOf<Session?>()
    val job =
      launch(UnconfinedTestDispatcher(testScheduler)) {
        ObserveSessionUseCase(sessions)().take(2).toList(emissions)
      }

    SignInUseCase(FakeAccountGateway { SignInResult.Success("1234", "shylock@example.com") }, sessions)()
    job.join()

    assertEquals(listOf(null, Session("1234", "shylock@example.com")), emissions)
  }
}
