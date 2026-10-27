package com.shashi.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.account.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Whether launch opens Home or Sign-in. [Loading] until the stored session has been read. */
sealed interface StartUiState {
  data object Loading : StartUiState

  data object SignedIn : StartUiState

  data object SignedOut : StartUiState
}

/**
 * Reads the stored session once at launch to pick the start destination (the sign-in gate). Later
 * session changes don't move it; Sign-in navigates to Home itself.
 */
@HiltViewModel
class StartViewModel @Inject constructor(observeSession: ObserveSessionUseCase) : ViewModel() {

  private val _uiState = MutableStateFlow<StartUiState>(StartUiState.Loading)
  val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val session = observeSession().first()
      _uiState.value = if (session != null) StartUiState.SignedIn else StartUiState.SignedOut
    }
  }
}
