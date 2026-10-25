package com.shashi.expensetracker

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Runs on the manifest's real ExpenseTrackerApp, so it exercises the Hilt graph from launch to screen.
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun launchShowsGreetingFromStubGateway() {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasTestTag("greeting_text")).fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithTag("greeting_text").assertTextEquals("Hello, Expense Tracker!")
  }
}
