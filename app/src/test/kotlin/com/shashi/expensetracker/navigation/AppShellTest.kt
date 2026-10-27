package com.shashi.expensetracker.navigation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlin.math.abs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val screenTags =
  mapOf(
    TopLevelDestination.Home to "screen_home",
    TopLevelDestination.Expenses to "screen_expenses",
    TopLevelDestination.Inbox to "screen_inbox",
    TopLevelDestination.Budgets to "screen_budgets",
    TopLevelDestination.Settings to "screen_settings",
  )

/**
 * The five-tab shell through the real activity and Hilt graph: launch, tab switch, Back, and the
 * pill nav or rail by width. FakeAccountModule gives a signed-in session, so launch opens Home.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class AppShellTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun coldStartSelectsHomeAndShowsGreeting() {
    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeRule.onNodeWithTag("greeting_text").assertIsDisplayed()
    assertOnlySelected(TopLevelDestination.Home)
  }

  @Test
  fun tappingEachTabShowsItsScreenAndMarksItActive() {
    TopLevelDestination.entries.forEach { tab ->
      composeRule.onNodeWithTag(tab.testTag).performClick()
      composeRule.onNodeWithTag(screenTags.getValue(tab)).assertIsDisplayed()
      assertOnlySelected(tab)
    }
  }

  @Test
  fun backFromAnotherTabReturnsHome() {
    composeRule.onNodeWithTag("nav_expenses").performClick()
    composeRule.onNodeWithTag("nav_budgets").performClick()
    composeRule.onNodeWithTag("screen_budgets").assertIsDisplayed()

    pressBack()

    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    assertOnlySelected(TopLevelDestination.Home)
  }

  @Test
  fun backOnHomeIsLeftToTheSystem() {
    composeRule.onNodeWithTag("nav_settings").performClick()
    pressBack()
    composeRule.onNodeWithTag("screen_home").assertIsDisplayed()

    composeRule.runOnIdle {
      assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }
  }

  @Test
  fun compactWidthShowsPillNavOnly() {
    composeRule.onNodeWithTag("pill_nav").assertIsDisplayed()
    composeRule.onNodeWithTag("nav_rail").assertDoesNotExist()
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp")
  fun wideWidthShowsRailInsteadOfPill() {
    composeRule.onNodeWithTag("nav_rail").assertIsDisplayed()
    composeRule.onNodeWithTag("pill_nav").assertDoesNotExist()

    composeRule.onNodeWithTag("nav_budgets").performClick()
    composeRule.onNodeWithTag("screen_budgets").assertIsDisplayed()
    assertOnlySelected(TopLevelDestination.Budgets)
  }

  private fun pressBack() {
    // Let the last navigation settle, so the NavHost's back callback is registered.
    composeRule.waitForIdle()
    composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()
  }

  private fun assertOnlySelected(active: TopLevelDestination) {
    TopLevelDestination.entries.forEach { tab ->
      val node = composeRule.onNodeWithTag(tab.testTag)
      if (tab == active) node.assertIsSelected() else node.assertIsNotSelected()
    }
  }
}

/** [PillNav] rendered directly: the badge, labels, item sizes and reduce motion. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class PillNavTest {

  @get:Rule val composeRule = createComposeRule()

  private fun setPillNav(badgeCount: Int, selected: TopLevelDestination = TopLevelDestination.Home) {
    composeRule.setContent {
      ExpenseTrackerTheme {
        PillNav(selected = selected, onSelect = {}, inboxBadgeCount = badgeCount)
      }
    }
  }

  @Test
  fun badgeIsHiddenAtZero() {
    setPillNav(badgeCount = 0)

    composeRule.onNodeWithTag("nav_inbox_badge", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun badgeShowsCountAndItemReadsLabelPlusNew() {
    setPillNav(badgeCount = 3)

    composeRule.onNodeWithTag("nav_inbox_badge", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithTag("nav_inbox").assertContentDescriptionEquals("Inbox, 3 new")
  }

  @Test
  fun activeInboxWithBadgeReadsLabelPlusNew() {
    setPillNav(badgeCount = 3, selected = TopLevelDestination.Inbox)

    composeRule
      .onNodeWithTag("nav_inbox")
      .assertIsSelected()
      .assertContentDescriptionContains("Inbox", substring = true)
      .assertContentDescriptionContains("3 new", substring = true)
      .assertContentDescriptionEquals("Inbox, 3 new")
  }

  @Test
  fun activeItemShowsLabelAndInactiveItemsHaveTalkBackLabels() {
    setPillNav(badgeCount = 0)

    composeRule.onNodeWithTag("nav_home").assert(hasText("Home"))
    composeRule.onNodeWithTag("nav_expenses").assertContentDescriptionEquals("Expenses")
    composeRule.onNodeWithTag("nav_settings").assertContentDescriptionEquals("Settings")
  }

  @Test
  fun itemsAreAtLeast48dpAndActiveIsAboutTwiceAsWide() {
    setPillNav(badgeCount = 0)

    TopLevelDestination.entries.forEach { tab ->
      composeRule.onNodeWithTag(tab.testTag).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }
    val active = composeRule.onNodeWithTag("nav_home").widthDp()
    val inactive = composeRule.onNodeWithTag("nav_inbox").widthDp()
    assertTrue("active $active vs inactive $inactive", abs(active - 2 * inactive) <= 1f)
  }

  @Test
  fun reduceMotionResizesItemsWithoutAnimation() {
    var selected by mutableStateOf(TopLevelDestination.Home)
    composeRule.setContent {
      ExpenseTrackerTheme(reduceMotion = true) {
        PillNav(selected = selected, onSelect = { selected = it })
      }
    }
    val activeWidth = composeRule.onNodeWithTag("nav_home").widthDp()

    composeRule.mainClock.autoAdvance = false
    composeRule.onNodeWithTag("nav_budgets").performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.mainClock.advanceTimeByFrame()

    assertTrue(abs(composeRule.onNodeWithTag("nav_budgets").widthDp() - activeWidth) <= 0.5f)
  }

  @Test
  @Config(fontScale = 1.75f)
  fun activeItemIsIconOnlyAtLargeFontButTalkBackReadsLabel() {
    setPillNav(badgeCount = 0, selected = TopLevelDestination.Expenses)

    composeRule.onNodeWithText("Expenses", useUnmergedTree = true).assertDoesNotExist()
    composeRule
      .onNodeWithTag("nav_expenses")
      .assertIsSelected()
      .assertContentDescriptionEquals("Expenses")
  }

  // Native graphics, because legacy Robolectric text measures near zero width and always fits.
  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Config(qualifiers = "w320dp-h800dp")
  fun activeLabelThatDoesNotFitAt320dpIsIconOnlyButTalkBackReadsLabel() {
    setPillNav(badgeCount = 0, selected = TopLevelDestination.Expenses)

    composeRule.onNodeWithText("Expenses", useUnmergedTree = true).assertDoesNotExist()
    composeRule
      .onNodeWithTag("nav_expenses")
      .assertIsSelected()
      .assertContentDescriptionEquals("Expenses")
  }

  @Test
  @Config(fontScale = 1.75f)
  fun activeRailItemIsIconOnlyAtLargeFontButTalkBackReadsLabel() {
    composeRule.setContent {
      ExpenseTrackerTheme {
        AppNavRail(selected = TopLevelDestination.Expenses, onSelect = {})
      }
    }

    composeRule.onNodeWithText("Expenses", useUnmergedTree = true).assertDoesNotExist()
    composeRule
      .onNodeWithTag("nav_expenses")
      .assertIsSelected()
      .assertContentDescriptionEquals("Expenses")
  }

  private fun SemanticsNodeInteraction.widthDp(): Float = getBoundsInRoot().width.value
}
