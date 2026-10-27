package com.shashi.expensetracker.domain.account

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SignInUseCaseTest {

  private val sessions = FakeSessionRepository()

  private fun useCase(signIn: () -> SignInResult) = SignInUseCase(FakeAccountGateway(signIn), sessions)

  @Test
  fun `success saves the session and returns success`() = runTest {
    val success = SignInResult.Success(accountId = "1234", email = "shylock@example.com")

    assertEquals(success, useCase { success }())

    assertEquals(Session(accountId = "1234", email = "shylock@example.com", sheetId = null), sessions.session.value)
  }

  @Test
  fun `cancel saves nothing`() = runTest {
    assertEquals(SignInResult.Cancelled, useCase { SignInResult.Cancelled }())

    assertNull(sessions.session.value)
    assertEquals(0, sessions.saveCount)
  }

  @Test
  fun `failure saves nothing`() = runTest {
    val failure = SignInResult.Failure(SignInFailure.NotConfigured)

    assertEquals(failure, useCase { failure }())

    assertNull(sessions.session.value)
    assertEquals(0, sessions.saveCount)
  }

  @Test
  fun `gateway exception becomes Unavailable and saves nothing`() = runTest {
    assertEquals(SignInResult.Failure(SignInFailure.Unavailable), useCase { error("boom") }())

    assertNull(sessions.session.value)
  }

  @Test
  fun `failed save becomes StorageFailed`() = runTest {
    sessions.failSave = true

    val result = useCase { SignInResult.Success("1234", "shylock@example.com") }()

    assertEquals(SignInResult.Failure(SignInFailure.StorageFailed), result)
    assertNull(sessions.session.value)
  }

  @Test
  fun `rethrows cancellation`() = runTest {
    try {
      useCase { throw CancellationException("cancelled") }()
      fail("Expected CancellationException")
    } catch (e: CancellationException) {
      assertEquals("cancelled", e.message)
    }
  }
}
