package com.shashi.expensetracker.domain.account

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoverExpenseSheetUseCaseTest {

  private val stored = Session("1234", "shylock@example.com", "stored-sheet")
  private val accounts = FakeAccountGateway { SignInResult.Cancelled }
  private val sheets = FakeExpenseSheetGateway()
  private val sessions = FakeSessionRepository(stored)

  private fun useCase(gateway: ExpenseSheetGateway = sheets) =
    RecoverExpenseSheetUseCase(gateway, sessions, EnsureExpenseSheetUseCase(accounts, gateway, sessions))

  private fun assertNoFindCreateOrSave() {
    assertEquals(0, sheets.findCalls)
    assertEquals(0, sheets.createCalls)
    assertEquals(0, sessions.saveCount)
  }

  @Test
  fun `present sheet is Ready with the same id and nothing else runs`() = runTest {
    assertEquals(EnsureExpenseSheetResult.Ready("stored-sheet"), useCase()())

    assertEquals(listOf("stored-sheet"), sheets.checkedIds)
    assertNoFindCreateOrSave()
    assertEquals(stored, sessions.session.value)
  }

  @Test
  fun `deleted sheet with none tagged creates one and stores its id`() = runTest {
    sheets.checkResult = ExpenseSheetResult.SheetMissing
    sheets.findResult = ExpenseSheetResult.NotFound

    assertEquals(EnsureExpenseSheetResult.Ready("created-sheet"), useCase()())

    assertEquals(listOf("stored-sheet"), sheets.checkedIds)
    assertEquals(1, sheets.findCalls)
    assertEquals(1, sheets.createCalls)
    assertEquals(Session("1234", "shylock@example.com", "created-sheet"), sessions.session.value)
  }

  @Test
  fun `trashed sheet with another tagged stores that one and creates nothing`() = runTest {
    sheets.checkResult = ExpenseSheetResult.SheetMissing
    sheets.findResult = ExpenseSheetResult.Ready("other-sheet")

    assertEquals(EnsureExpenseSheetResult.Ready("other-sheet"), useCase()())

    assertEquals(0, sheets.createCalls)
    assertEquals("other-sheet", sessions.session.value?.sheetId)
  }

  @Test
  fun `no session is an Access failure and the gateway is not called`() = runTest {
    sessions.session.value = null

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access), useCase()())

    assertEquals(0, sheets.checkCalls)
    assertNoFindCreateOrSave()
  }

  @Test
  fun `session without a sheet id is an Access failure and the gateway is not called`() = runTest {
    sessions.session.value = stored.copy(sheetId = null)

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access), useCase()())

    assertEquals(0, sheets.checkCalls)
    assertNoFindCreateOrSave()
  }

  @Test
  fun `failed check returns the same failure and leaves the session unchanged`() = runTest {
    listOf(ExpenseSheetFailure.Network, ExpenseSheetFailure.Access, ExpenseSheetFailure.Unexpected).forEach { reason ->
      sheets.checkResult = ExpenseSheetResult.Failure(reason)

      assertEquals(EnsureExpenseSheetResult.Failure(reason), useCase()())
    }

    assertNoFindCreateOrSave()
    assertEquals(stored, sessions.session.value)
  }

  /** [sheets], except `check` needs consent for its first [needsConsentTimes] calls. */
  private fun consentingGateway(needsConsentTimes: Int) =
    object : ExpenseSheetGateway by sheets {
      var checks = 0

      override suspend fun check(sheetId: String): ExpenseSheetResult {
        checks++
        return if (checks <= needsConsentTimes) {
          ExpenseSheetResult.NeedsConsent(object : ConsentRequest {})
        } else {
          sheets.check(sheetId)
        }
      }
    }

  @Test
  fun `check needing consent asks once and retries`() = runTest {
    val gateway = consentingGateway(needsConsentTimes = 1)

    assertEquals(EnsureExpenseSheetResult.Ready("stored-sheet"), useCase(gateway)())

    assertEquals(1, accounts.consentRequests)
    assertEquals(2, gateway.checks)
    assertNoFindCreateOrSave()
  }

  @Test
  fun `check needing consent declined is ConsentDeclined and saves nothing`() = runTest {
    val gateway = consentingGateway(needsConsentTimes = 1)
    accounts.consentGranted = false

    assertEquals(EnsureExpenseSheetResult.ConsentDeclined, useCase(gateway)())

    assertEquals(1, accounts.consentRequests)
    assertEquals(1, gateway.checks)
    assertNoFindCreateOrSave()
    assertEquals(stored, sessions.session.value)
  }

  @Test
  fun `consent granted on check whose retry is SheetMissing finds or creates and stores the new id`() = runTest {
    val gateway = consentingGateway(needsConsentTimes = 1)
    sheets.checkResult = ExpenseSheetResult.SheetMissing
    sheets.findResult = ExpenseSheetResult.NotFound

    assertEquals(EnsureExpenseSheetResult.Ready("created-sheet"), useCase(gateway)())

    assertEquals(1, accounts.consentRequests)
    assertEquals(2, gateway.checks)
    assertEquals(1, sheets.findCalls)
    assertEquals(1, sheets.createCalls)
    assertEquals(Session("1234", "shylock@example.com", "created-sheet"), sessions.session.value)
  }

  @Test
  fun `check still needing consent after a grant is an Access failure`() = runTest {
    val gateway = consentingGateway(needsConsentTimes = 2)

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.Access), useCase(gateway)())

    assertEquals(2, gateway.checks)
    assertNoFindCreateOrSave()
  }

  @Test
  fun `missing sheet with a failing save is StorageFailed`() = runTest {
    sheets.checkResult = ExpenseSheetResult.SheetMissing
    sessions.failSave = true

    assertEquals(EnsureExpenseSheetResult.Failure(ExpenseSheetFailure.StorageFailed), useCase()())

    assertEquals(stored, sessions.session.value)
    assertEquals(listOf("stored-sheet"), sheets.checkedIds)
    assertEquals(1, sheets.findCalls)
  }
}
