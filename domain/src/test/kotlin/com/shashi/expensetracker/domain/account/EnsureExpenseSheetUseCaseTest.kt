package com.shashi.expensetracker.domain.account

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnsureExpenseSheetUseCaseTest {

  private val accounts = FakeAccountGateway { SignInResult.Cancelled }
  private val sheets = FakeExpenseSheetGateway()
  private val sessions = FakeSessionRepository()
  private val useCase = EnsureExpenseSheetUseCase(accounts, sheets, sessions)
  private val consent = AccessResult.NeedsUserConsent(object : ConsentRequest {})

  private suspend fun run() = useCase("1234", "shylock@example.com")

  @Test
  fun `existing sheet is reused and never created`() = runTest {
    sheets.findResult = ExpenseSheetResult.Ready("old-sheet")

    assertEquals(EnsureExpenseSheetResult.Ready("old-sheet"), run())

    assertEquals(0, sheets.createCalls)
    assertEquals(Session("1234", "shylock@example.com", "old-sheet"), sessions.session.value)
  }

  @Test
  fun `no sheet creates one and stores its id`() = runTest {
    sheets.findResult = ExpenseSheetResult.NotFound

    assertEquals(EnsureExpenseSheetResult.Ready("created-sheet"), run())

    assertEquals(1, sheets.createCalls)
    assertEquals("created-sheet", sessions.session.value?.sheetId)
  }

  @Test
  fun `consent granted continues`() = runTest {
    accounts.access = consent

    assertEquals(EnsureExpenseSheetResult.Ready("test-sheet"), run())

    assertEquals(1, accounts.consentRequests)
  }

  @Test
  fun `consent declined stores nothing and does not touch the sheet`() = runTest {
    accounts.access = consent
    accounts.consentGranted = false

    assertEquals(EnsureExpenseSheetResult.ConsentDeclined, run())

    assertEquals(0, sheets.findCalls)
    assertNull(sessions.session.value)
  }

  @Test
  fun `access failure stores nothing`() = runTest {
    accounts.access = AccessResult.Failure(AccessFailure.Unavailable)

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access), run())

    assertNull(sessions.session.value)
  }

  /** Returns NeedsConsent for the first [needsConsentTimes] finds, then the sheet. */
  private class ConsentingGateway(var needsConsentTimes: Int) : ExpenseSheetGateway {
    var findCalls = 0

    override suspend fun find(): ExpenseSheetResult {
      findCalls++
      return if (findCalls <= needsConsentTimes) {
        ExpenseSheetResult.NeedsConsent(object : ConsentRequest {})
      } else {
        ExpenseSheetResult.Ready("sheet-after-consent")
      }
    }

    override suspend fun create(): ExpenseSheetResult = error("not used")

    override suspend fun check(sheetId: String): ExpenseSheetResult = error("not used")
  }

  @Test
  fun `find needing consent granted retries and saves`() = runTest {
    val gateway = ConsentingGateway(needsConsentTimes = 1)

    val result = EnsureExpenseSheetUseCase(accounts, gateway, sessions)("1234", "shylock@example.com")

    assertEquals(EnsureExpenseSheetResult.Ready("sheet-after-consent"), result)
    assertEquals(2, gateway.findCalls)
    assertEquals("sheet-after-consent", sessions.session.value?.sheetId)
  }

  @Test
  fun `find needing consent declined is ConsentDeclined and saves nothing`() = runTest {
    val gateway = ConsentingGateway(needsConsentTimes = 1)
    accounts.consentGranted = false

    val result = EnsureExpenseSheetUseCase(accounts, gateway, sessions)("1234", "shylock@example.com")

    assertEquals(EnsureExpenseSheetResult.ConsentDeclined, result)
    assertEquals(1, gateway.findCalls)
    assertNull(sessions.session.value)
  }

  @Test
  fun `retry still needing consent after a grant is an Access failure`() = runTest {
    val gateway = ConsentingGateway(needsConsentTimes = 2)

    val result = EnsureExpenseSheetUseCase(accounts, gateway, sessions)("1234", "shylock@example.com")

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access), result)
    assertEquals(2, gateway.findCalls)
    assertNull(sessions.session.value)
  }

  @Test
  fun `create failure stores nothing`() = runTest {
    sheets.findResult = ExpenseSheetResult.NotFound
    sheets.createResult = ExpenseSheetResult.Failure(ExpenseSheetFailure.Network)

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Network), run())

    assertEquals(0, sessions.saveCount)
  }

  @Test
  fun `failed save is StorageFailed`() = runTest {
    sessions.failSave = true

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.StorageFailed), run())
  }
}
