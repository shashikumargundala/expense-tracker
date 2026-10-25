package com.shashi.expensetracker.budget

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class BudgetsViewModel @Inject constructor() : ViewModel() {

  private val _uiState = MutableStateFlow(BudgetsUiState())
  val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()
}
