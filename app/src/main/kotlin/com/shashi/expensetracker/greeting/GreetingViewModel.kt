package com.shashi.expensetracker.greeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.greeting.GetGreetingUseCase
import com.shashi.expensetracker.domain.greeting.GreetingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GreetingViewModel @Inject constructor(private val getGreeting: GetGreetingUseCase) :
  ViewModel() {

  private val _uiState = MutableStateFlow(GreetingUiState())
  val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val message =
        when (val result = getGreeting()) {
          is GreetingResult.Success -> result.greeting
          GreetingResult.NameUnavailable -> GreetingUiState.FALLBACK_MESSAGE
        }
      _uiState.value = GreetingUiState(message = message)
    }
  }
}
