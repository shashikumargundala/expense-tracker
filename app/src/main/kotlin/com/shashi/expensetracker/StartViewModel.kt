package com.shashi.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.account.ObserveSessionUseCase
import com.shashi.expensetracker.domain.sync.RequestSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Whether launch opens Home or Sign-in. [Loading] until the stored session has been read. */
sealed interface StartUiState {
  data object Loading : StartUiState

  data object SignedIn : StartUiState

  data object SignedOut : StartUiState
}

/**
 * Reads the stored session at launch to pick the start destination (the sign-in gate). Home opens
 * only for a session that has an Expense Sheet ID; an older one without it shows Sign-in. Later
 * session changes don't move it; Sign-in navigates to Home itself.
 *
 * It also asks for a sync each time the session first has a Sheet ID (AD-14 "on app open"): on a
 * cold start that opens Home, and after the first sign-in stores the Sheet ID.
 */
@HiltViewModel
class StartViewModel
@Inject
constructor(observeSession: ObserveSessionUseCase, private val requestSync: RequestSyncUseCase) :
  ViewModel() {

  private val _uiState = MutableStateFlow<StartUiState>(StartUiState.Loading)
  val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      observeSession().map { it?.sheetId }.distinctUntilChanged().collect { sheetId ->
        if (_uiState.value == StartUiState.Loading) {
          _uiState.value = if (sheetId != null) StartUiState.SignedIn else StartUiState.SignedOut
        }
        if (sheetId != null) requestSync()
      }
    }
  }
}
