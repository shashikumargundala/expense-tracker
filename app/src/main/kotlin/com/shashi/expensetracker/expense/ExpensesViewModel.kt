package com.shashi.expensetracker.expense

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ExpensesViewModel @Inject constructor() : ViewModel() {

  private val _uiState = MutableStateFlow(ExpensesUiState())
  val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()
}
