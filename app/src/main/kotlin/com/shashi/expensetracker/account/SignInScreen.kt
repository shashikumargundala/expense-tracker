package com.shashi.expensetracker.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.R
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/** Sign-in wired to its ViewModel. Calls [onSignedIn] once the session is stored. */
@Composable
fun SignInRoute(
  contentPadding: PaddingValues,
  onSignedIn: () -> Unit,
  viewModel: SignInViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val currentOnSignedIn by rememberUpdatedState(onSignedIn)
  LaunchedEffect(uiState.signedIn) { if (uiState.signedIn) currentOnSignedIn() }
  SignInScreen(
    uiState = uiState,
    onContinueWithGoogle = viewModel::onContinueWithGoogle,
    contentPadding = contentPadding,
  )
}

/** Minimal Sign-in: title, Continue with Google, progress and error. Story 2.5 designs it. */
@Composable
fun SignInScreen(
  uiState: SignInUiState,
  onContinueWithGoogle: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("screen_signin")
        .padding(contentPadding)
        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4),
    verticalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    if (uiState.inProgress) {
      CircularProgressIndicator(modifier = Modifier.testTag("signin_progress"))
    } else {
      Button(
        onClick = onContinueWithGoogle,
        modifier = Modifier.testTag("signin_continue_google"),
      ) {
        Text(stringResource(R.string.signin_continue_google))
      }
    }
    uiState.error?.let { error ->
      Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("signin_error"),
      )
    }
  }
}

private fun SignInError.messageRes(): Int =
  when (this) {
    SignInError.CouldNotSignIn -> R.string.signin_error_generic
    SignInError.NotConfigured -> R.string.signin_error_not_configured
  }

@Preview
@Composable
private fun SignInScreenPreview() {
  ExpenseTrackerTheme {
    SignInScreen(uiState = SignInUiState(error = SignInError.CouldNotSignIn), onContinueWithGoogle = {})
  }
}
