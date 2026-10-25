package com.shashi.expensetracker.inbox

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class InboxViewModel @Inject constructor() : ViewModel() {

  private val _uiState = MutableStateFlow(InboxUiState())
  val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
}
