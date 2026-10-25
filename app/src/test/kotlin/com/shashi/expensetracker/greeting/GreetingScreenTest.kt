package com.shashi.expensetracker.greeting

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// A plain Application keeps the Hilt graph out of this stateless screen test.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class GreetingScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsGreetingText() {
    composeRule.setContent {
      MaterialTheme { GreetingScreen(uiState = GreetingUiState(message = "Hello, Shashi!")) }
    }

    composeRule
      .onNodeWithTag("greeting_text")
      .assertIsDisplayed()
      .assertTextEquals("Hello, Shashi!")
  }

  @Test
  fun showsLoadingIndicatorBeforeGreetingArrives() {
    composeRule.setContent { MaterialTheme { GreetingScreen(uiState = GreetingUiState()) } }

    composeRule.onNodeWithTag("greeting_loading").assertIsDisplayed()
  }
}
