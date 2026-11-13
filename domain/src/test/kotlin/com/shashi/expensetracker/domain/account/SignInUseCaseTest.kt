package com.shashi.expensetracker.domain.account

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SignInUseCaseTest {

  private val sessions = FakeSessionRepository()
  private val sheets = FakeExpenseSheetGateway()
  private val success = SignInResult.Success(accountId = "1234", email = "shylock@example.com")

  private fun useCase(signIn: () -> SignInResult): SignInUseCase {
    val gateway = FakeAccountGateway(signIn)
    return SignInUseCase(gateway, EnsureExpenseSheetUseCase(gateway, sheets, sessions))
  }

  @Test
  fun `success saves the session with its sheet id once`() = runTest {
    assertEquals(success, useCase { success }())

    assertEquals(Session("1234", "shylock@example.com", sheetId = "test-sheet"), sessions.session.value)
    assertEquals(1, sessions.saveCount)
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

    assertEquals(SignInResult.Failure(SignInFailure.StorageFailed), useCase { success }())

    assertNull(sessions.session.value)
  }

  @Test
  fun `sheet failure becomes Unavailable and saves nothing`() = runTest {
    sheets.findResult = ExpenseSheetResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(SignInResult.Failure(SignInFailure.Unavailable), useCase { success }())

    assertEquals(0, sessions.saveCount)
  }

  @Test
  fun `declined consent is reported as cancelled and saves nothing`() = runTest {
    val gateway = FakeAccountGateway { success }
    gateway.access = AccessResult.NeedsUserConsent(object : ConsentRequest {})
    gateway.consentGranted = false

    val result = SignInUseCase(gateway, EnsureExpenseSheetUseCase(gateway, sheets, sessions))()

    assertEquals(SignInResult.Cancelled, result)
    assertEquals(0, sessions.saveCount)
    assertEquals(0, sheets.findCalls)
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
