package com.shashi.expensetracker.home

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
class HomeViewModel @Inject constructor(private val getGreeting: GetGreetingUseCase) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val greeting =
        when (val result = getGreeting()) {
          is GreetingResult.Success -> result.greeting
          GreetingResult.NameUnavailable -> HomeUiState.FALLBACK_GREETING
        }
      _uiState.value = HomeUiState(greeting = greeting)
    }
  }
}
