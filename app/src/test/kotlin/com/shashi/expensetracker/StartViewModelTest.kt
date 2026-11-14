package com.shashi.expensetracker

import com.shashi.expensetracker.account.FakeSessionRepository
import com.shashi.expensetracker.account.SignedInTestSession
import com.shashi.expensetracker.domain.account.ObserveSessionUseCase
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.sync.RequestSyncUseCase
import com.shashi.expensetracker.domain.sync.SyncController
import com.shashi.expensetracker.domain.sync.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

  private class CountingSyncController : SyncController {
    var requests = 0

    override fun request() {
      requests++
    }

    override fun observeState(): Flow<SyncState> = flowOf(SyncState())
  }

  private val sessions = FakeSessionRepository()
  private val sync = CountingSyncController()

  @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After fun tearDown() = Dispatchers.resetMain()

  private fun viewModel() = StartViewModel(ObserveSessionUseCase(sessions), RequestSyncUseCase(sync))

  @Test
  fun `a cold start with a Sheet opens Home and asks for one sync`() {
    val vm = viewModel()

    assertEquals(StartUiState.SignedIn, vm.uiState.value)
    assertEquals(1, sync.requests)

    // Another save of the same Sheet doesn't ask again.
    sessions.session.value = SignedInTestSession.copy(email = "other@example.com")
    assertEquals(1, sync.requests)
  }

  @Test
  fun `signed out asks for nothing until sign-in stores a Sheet ID`() {
    sessions.session.value = null
    val vm = viewModel()

    assertEquals(StartUiState.SignedOut, vm.uiState.value)
    assertEquals(0, sync.requests)

    sessions.session.value = Session("acct", "tester@example.com", sheetId = "sheet-1")

    assertEquals(1, sync.requests)
    // The gate doesn't move; Sign-in navigates itself.
    assertEquals(StartUiState.SignedOut, vm.uiState.value)
  }
}
