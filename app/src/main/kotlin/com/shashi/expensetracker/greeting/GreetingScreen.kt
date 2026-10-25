package com.shashi.expensetracker.greeting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GreetingScreen(uiState: GreetingUiState, modifier: Modifier = Modifier) {
  Surface(modifier = modifier.fillMaxSize()) {
    Box(contentAlignment = Alignment.Center) {
      val message = uiState.message
      if (message == null) {
        CircularProgressIndicator(modifier = Modifier.testTag("greeting_loading"))
      } else {
        Text(
          text = message,
          style = MaterialTheme.typography.headlineMedium,
          modifier = Modifier.testTag("greeting_text"),
        )
      }
    }
  }
}

@Preview
@Composable
private fun GreetingScreenPreview() {
  MaterialTheme { GreetingScreen(uiState = GreetingUiState(message = "Hello, Expense Tracker!")) }
}
