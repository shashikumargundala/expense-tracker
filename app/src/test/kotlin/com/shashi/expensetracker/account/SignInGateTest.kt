package com.shashi.expensetracker.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import com.shashi.expensetracker.domain.account.SignInFailure
import com.shashi.expensetracker.domain.account.SignInResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The sign-in gate through the real activity: no session, success, cancel and failure. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class SignInGateTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

  @Inject lateinit var sessions: FakeSessionRepository
  @Inject lateinit var gateway: FakeAccountGateway

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

  private fun launch() {
    scenario = ActivityScenario.launch(MainActivity::class.java)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasTestTag("screen_signin")).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun noSessionShowsSignInWithNoNav() {
    launch()

    composeRule.onNodeWithTag("screen_signin").assertIsDisplayed()
    composeRule.onNodeWithTag("signin_continue_google").assertIsDisplayed()
    composeRule.onNodeWithTag("pill_nav").assertDoesNotExist()
    composeRule.onNodeWithTag("nav_rail").assertDoesNotExist()
    composeRule.onNodeWithTag("screen_home").assertDoesNotExist()
  }

  @Test
  fun successfulSignInSavesSessionAndOpensHome() {
    gateway.result = SignInResult.Success(accountId = "1234", email = "shylock@example.com")
    launch()

    composeRule.onNodeWithTag("signin_continue_google").performClick()

    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeRule.onNodeWithTag("pill_nav").assertIsDisplayed()
    composeRule.onNodeWithTag("screen_signin").assertDoesNotExist()
    assertEquals("1234", sessions.session.value?.accountId)
    assertEquals("shylock@example.com", sessions.session.value?.email)
  }

  @Test
  fun backFromHomeAfterSignInLeavesToTheSystem() {
    launch()
    composeRule.onNodeWithTag("signin_continue_google").performClick()
    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()

    scenario!!.onActivity { activity ->
      assertEquals(false, activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }
  }

  @Test
  fun cancelStaysOnSignInWithNoMessage() {
    gateway.result = SignInResult.Cancelled
    launch()

    composeRule.onNodeWithTag("signin_continue_google").performClick()

    composeRule.onNodeWithTag("screen_signin").assertIsDisplayed()
    composeRule.onNodeWithTag("signin_continue_google").assertIsDisplayed()
    composeRule.onNodeWithTag("signin_error").assertDoesNotExist()
    composeRule.onNodeWithTag("screen_home").assertDoesNotExist()
    assertEquals(1, gateway.calls)
    assertNull(sessions.session.value)
  }

  @Test
  fun failureStaysOnSignInAndShowsError() {
    gateway.result = SignInResult.Failure(SignInFailure.Unavailable)
    launch()

    composeRule.onNodeWithTag("signin_continue_google").performClick()

    composeRule.onNodeWithTag("signin_error").assertIsDisplayed()
    composeRule.onNodeWithTag("signin_continue_google").assertIsDisplayed()
    composeRule.onNodeWithTag("screen_home").assertDoesNotExist()
    assertNull(sessions.session.value)
  }

  @Test
  fun blankClientIdFailureShowsErrorNotCrash() {
    gateway.result = SignInResult.Failure(SignInFailure.NotConfigured)
    launch()

    composeRule.onNodeWithTag("signin_continue_google").performClick()

    composeRule.onNodeWithTag("signin_error").assertIsDisplayed()
    composeRule.onNodeWithTag("screen_signin").assertIsDisplayed()
  }
}
