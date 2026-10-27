package com.shashi.expensetracker.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Baselines of the five-tab shell, through the real activity and Hilt graph: Home in each variant,
 * plus Expenses on a 320dp screen, where the active label doesn't fit and the item is icon only.
 * FakeAccountModule gives a signed-in session, so launch opens Home.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class ShellScreenshotTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun shellHomeLight() = captureHome("shell_home_light")

  @Test
  @Config(qualifiers = "+night")
  fun shellHomeDark() = captureHome("shell_home_dark")

  @Test
  @Config(fontScale = 1.75f)
  fun shellHomeFont175() = captureHome("shell_home_font175")

  @Test
  @Config(qualifiers = "w600dp-h900dp")
  fun shellHomeWide() = captureHome("shell_home_wide")

  @Test
  @Config(qualifiers = "w320dp-h800dp")
  fun shellExpensesW320() {
    composeRule.onNodeWithTag("nav_expenses").performClick()
    composeRule.onNodeWithTag("screen_expenses").assertIsDisplayed()
    composeRule.captureScreen("shell_expenses_w320")
  }

  private fun captureHome(name: String) {
    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeRule.captureScreen(name)
  }
}
