package com.shashi.expensetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// The real Hilt graph from launch to screen, except the account bindings: FakeAccountModule gives a
// signed-in session, so launch opens Home.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class MainActivityTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun launchShowsHomeWithGreetingFromStubGateway() {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasTestTag("greeting_text")).fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeRule.onNodeWithTag("greeting_text").assertTextEquals("Hello, Expense Tracker!")
  }
}
