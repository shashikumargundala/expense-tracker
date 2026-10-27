package com.shashi.expensetracker.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import com.shashi.expensetracker.domain.account.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel @Inject constructor(private val signIn: SignInUseCase) : ViewModel() {

  private val _uiState = MutableStateFlow(SignInUiState())
  val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

  /** Continue with Google. Ignored while an attempt is running or after success. */
  fun onContinueWithGoogle() {
    val current = _uiState.value
    if (current.inProgress || current.signedIn) return
    _uiState.value = current.copy(inProgress = true, error = null)
    viewModelScope.launch {
      val result = signIn()
      _uiState.update {
        when (result) {
          is SignInResult.Success -> it.copy(inProgress = false, signedIn = true)
          // Cancel is silent: back to the idle Sign-in screen.
          SignInResult.Cancelled -> it.copy(inProgress = false)
          is SignInResult.Failure -> it.copy(inProgress = false, error = result.reason.toUiError())
        }
      }
    }
  }

  private fun SignInFailure.toUiError(): SignInError =
    if (this == SignInFailure.NotConfigured) SignInError.NotConfigured else SignInError.CouldNotSignIn
}
