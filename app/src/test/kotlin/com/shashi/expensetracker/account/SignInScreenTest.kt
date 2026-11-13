package com.shashi.expensetracker.account

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless Sign-in screen in each state of the story 2.5 matrix. */
// A plain Application keeps the Hilt graph out of this stateless screen test.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class SignInScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private var continueClicks = 0

  private fun show(uiState: SignInUiState) {
    composeRule.setContent {
      ExpenseTrackerTheme { SignInScreen(uiState = uiState, onContinueWithGoogle = { continueClicks++ }) }
    }
  }

  @Test
  fun idleShowsTitleButtonAndPrivacyLine() {
    show(SignInUiState())

    composeRule.onNodeWithTag("screen_signin").assertIsDisplayed()
    composeRule.onNodeWithText("Expense Tracker").assertIsDisplayed()
    composeRule
      .onNodeWithTag("signin_continue_google")
      .assertIsDisplayed()
      .assertTextEquals("Continue with Google")
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    composeRule.onNodeWithText("Private: lives in your own Google Sheet").assertIsDisplayed()
    composeRule.onNodeWithTag("signin_progress", useUnmergedTree = true).assertDoesNotExist()
    composeRule.onNodeWithTag("signin_error").assertDoesNotExist()
  }

  @Test
  fun idleAlreadyHasTheEmptyPoliteLiveRegion() {
    show(SignInUiState())

    composeRule
      .onNodeWithTag("signin_status")
      .assertExists()
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
      .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
  }

  @Test
  fun theSameLiveRegionGainsTheSettingUpTextWhenSigningInStarts() {
    var uiState by mutableStateOf(SignInUiState())
    composeRule.setContent { ExpenseTrackerTheme { SignInScreen(uiState = uiState, onContinueWithGoogle = {}) } }
    val idleId = composeRule.onNodeWithTag("signin_status").fetchSemanticsNode().id

    uiState = SignInUiState(inProgress = true)

    composeRule
      .onNodeWithTag("signin_status")
      .assertTextEquals("Setting up your Expense Sheet…")
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    assertEquals(idleId, composeRule.onNodeWithTag("signin_status").fetchSemanticsNode().id)
  }

  @Test
  fun buttonCallsOnContinue() {
    show(SignInUiState())

    composeRule.onNodeWithTag("signin_continue_google").performClick()

    assertEquals(1, continueClicks)
  }

  @Test
  fun buttonIsAtLeastTheMinimumTouchTarget() {
    show(SignInUiState())

    val bounds = composeRule.onNodeWithTag("signin_continue_google").fetchSemanticsNode().boundsInRoot
    val minPx = with(composeRule.density) { 48.dp.toPx() }
    assertTrue("button height ${bounds.height}px < $minPx px", bounds.height >= minPx)
  }

  @Test
  fun progressReplacesTheButtonWithAPoliteAnnouncement() {
    show(SignInUiState(inProgress = true))

    composeRule.onNodeWithTag("signin_continue_google").assertDoesNotExist()
    composeRule.onNodeWithTag("signin_progress", useUnmergedTree = true).assertIsDisplayed()
    composeRule
      .onNodeWithTag("signin_status")
      .assertIsDisplayed()
      .assertTextEquals("Setting up your Expense Sheet…")
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    composeRule.onNodeWithTag("signin_progress_text", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("Private: lives in your own Google Sheet").assertIsDisplayed()
  }

  @Test
  fun couldNotSignInShowsTheGenericErrorWithTheButton() {
    show(SignInUiState(error = SignInError.CouldNotSignIn))

    composeRule
      .onNodeWithTag("signin_error")
      .assertIsDisplayed()
      .assertTextEquals("Couldn't sign in. Check your connection and try again.")
    composeRule.onNodeWithTag("signin_continue_google").performClick()
    assertEquals(1, continueClicks)
  }

  @Test
  fun notConfiguredShowsItsOwnError() {
    show(SignInUiState(error = SignInError.NotConfigured))

    composeRule.onNodeWithTag("signin_error").assertIsDisplayed().assertTextEquals("Sign-in isn't set up in this build.")
    composeRule.onNodeWithTag("signin_continue_google").assertIsDisplayed()
  }

  @Test
  fun errorTextPairMeetsAAInLightAndDark() {
    val schemes = mutableListOf<ColorScheme>()
    composeRule.setContent {
      ExpenseTrackerTheme(darkTheme = false) { schemes += MaterialTheme.colorScheme }
      ExpenseTrackerTheme(darkTheme = true) { schemes += MaterialTheme.colorScheme }
    }
    composeRule.waitForIdle()

    listOf("light" to schemes[0], "dark" to schemes[1]).forEach { (mode, scheme) ->
      val ratio = contrast(scheme.onErrorContainer, scheme.errorContainer)
      assertTrue("$mode error text (onErrorContainer on errorContainer) is $ratio:1", ratio >= 4.5)
    }
  }

  /** WCAG 2 contrast ratio of two opaque colors. */
  private fun contrast(a: Color, b: Color): Double {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05) / (lo + 0.05)
  }
}
