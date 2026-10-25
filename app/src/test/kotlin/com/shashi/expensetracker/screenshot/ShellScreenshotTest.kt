package com.shashi.expensetracker.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Baselines of the five-tab shell on Home, through the real activity and Hilt graph. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class ShellScreenshotTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

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

  private fun captureHome(name: String) {
    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeRule.captureScreen(name)
  }
}
