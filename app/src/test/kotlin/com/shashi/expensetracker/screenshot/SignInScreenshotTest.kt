package com.shashi.expensetracker.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import com.shashi.expensetracker.account.FakeSessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Baselines of Sign-in, signed out, through the real activity and Hilt graph: light and dark at
 * 360dp, and light at 320dp with 1.75x font. FakeAccountModule starts signed in, so each test clears
 * the session before it launches MainActivity (hence an empty compose rule plus ActivityScenario).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class SignInScreenshotTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

  @Inject lateinit var sessions: FakeSessionRepository

  private var scenario: ActivityScenario<MainActivity>? = null

  @Before
  fun setUp() {
    hiltRule.inject()
    sessions.session.value = null
  }

  @After
  fun tearDown() {
    scenario?.close()
  }

  @Test
  fun signInLight() = captureSignIn("signin_light")

  @Test
  @Config(qualifiers = "+night")
  fun signInDark() = captureSignIn("signin_dark")

  @Test
  @Config(qualifiers = "w320dp-h800dp", fontScale = 1.75f)
  fun signInFont175() = captureSignIn("signin_font175")

  private fun captureSignIn(name: String) {
    scenario = ActivityScenario.launch(MainActivity::class.java)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasTestTag("screen_signin")).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("signin_continue_google").assertIsDisplayed()
    composeRule.captureScreen(name)
  }
}
