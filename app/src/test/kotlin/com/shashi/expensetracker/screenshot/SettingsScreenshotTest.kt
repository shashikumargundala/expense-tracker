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
 * Baselines of Settings with the Google account tile, through the real activity and Hilt graph:
 * light and dark at 360dp, and light at 320dp with 1.75x font. FakeAccountModule's signed-in session
 * (`tester@example.com`, `test-sheet`) fills the tile.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class SettingsScreenshotTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun settingsLight() = captureSettings("settings_light")

  @Test
  @Config(qualifiers = "+night")
  fun settingsDark() = captureSettings("settings_dark")

  @Test
  @Config(qualifiers = "w320dp-h800dp", fontScale = 1.75f)
  fun settingsFont175() = captureSettings("settings_font175")

  private fun captureSettings(name: String) {
    composeRule.onNodeWithTag("nav_settings").performClick()
    composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_open_sheet").assertIsDisplayed()
    composeRule.captureScreen(name)
  }
}
