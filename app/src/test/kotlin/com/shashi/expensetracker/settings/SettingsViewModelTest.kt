package com.shashi.expensetracker.settings

import com.shashi.expensetracker.domain.account.ObserveSessionUseCase
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  /** Emits only what a test sends, so "not yet emitted" can be observed. */
  private class FakeSessions : SessionRepository {
    val sessions = MutableSharedFlow<Session?>(extraBufferCapacity = 1)

    override fun observe(): Flow<Session?> = sessions

    override suspend fun save(session: Session): Boolean = true
  }

  private val sessions = FakeSessions()

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel() = SettingsViewModel(ObserveSessionUseCase(sessions))

  @Test
  fun `shows nothing before the session emits`() {
    val viewModel = viewModel()

    assertEquals(SettingsUiState(), viewModel.uiState.value)
    assertEquals(false, viewModel.uiState.value.loaded)
  }

  @Test
  fun `shows the signed-in account and its Sheet`() {
    val viewModel = viewModel()

    sessions.sessions.tryEmit(Session("id", "shylock@gmail.com", "sheet-1"))

    val state = viewModel.uiState.value
    assertEquals(
      SettingsUiState(loaded = true, email = "shylock@gmail.com", sheetId = "sheet-1"),
      state,
    )
    assertEquals("S", state.initial)
  }

  @Test
  fun `keeps the account without a Sheet ID`() {
    val viewModel = viewModel()

    sessions.sessions.tryEmit(Session("id", "tester@example.com", sheetId = null))

    assertEquals(
      SettingsUiState(loaded = true, email = "tester@example.com", sheetId = null),
      viewModel.uiState.value,
    )
  }
}
