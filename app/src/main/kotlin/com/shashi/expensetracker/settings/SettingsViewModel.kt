package com.shashi.expensetracker.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.account.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Shows the stored session's account and Expense Sheet, following every session change. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val observeSession: ObserveSessionUseCase) :
  ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      observeSession().collect { session ->
        _uiState.value =
          SettingsUiState(loaded = true, email = session?.email, sheetId = session?.sheetId)
      }
    }
  }
}
