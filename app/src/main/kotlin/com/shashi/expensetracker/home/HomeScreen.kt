package com.shashi.expensetracker.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/** The Home tab, wired to its ViewModel. [contentPadding] keeps content clear of bars and nav. */
@Composable
fun HomeRoute(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HomeScreen(uiState = uiState, contentPadding = contentPadding)
}

/** Home placeholder: the header greeting. The hero chart and Category list come in later epics. */
@Composable
fun HomeScreen(
  uiState: HomeUiState,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("screen_home")
        .padding(contentPadding)
        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4)
  ) {
    val greeting = uiState.greeting
    if (greeting == null) {
      CircularProgressIndicator(modifier = Modifier.testTag("greeting_loading"))
    } else {
      Text(
        text = greeting,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag("greeting_text"),
      )
    }
  }
}

@Preview
@Composable
private fun HomeScreenPreview() {
  ExpenseTrackerTheme { HomeScreen(uiState = HomeUiState(greeting = "Hello, Expense Tracker!")) }
}
