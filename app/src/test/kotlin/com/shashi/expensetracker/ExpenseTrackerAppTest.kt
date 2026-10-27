package com.shashi.expensetracker

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.ResumedActivityTracker
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on the manifest's real [ExpenseTrackerApp] and real Hilt graph (no fakes, a fresh Room
 * database): launch with no session opens Sign-in, and the app registered the resumed-activity
 * tracker Credential Manager needs.
 */
@RunWith(AndroidJUnit4::class)
class ExpenseTrackerAppTest {

  @get:Rule val composeRule = createEmptyComposeRule()

  @Test
  fun launchWithNoSessionShowsSignInAndTrackerHoldsTheResumedActivity() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodes(hasTestTag("screen_signin")).fetchSemanticsNodes().isNotEmpty()
      }

      // The real app's injected set holds the singleton tracker. A test-source Hilt EntryPoint
      // can't be used: it isn't part of the component generated for the main ExpenseTrackerApp.
      val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
      val tracker = app.activityCallbacks.filterIsInstance<ResumedActivityTracker>().single()
      scenario.onActivity { activity -> assertSame(activity, tracker.current) }
    }
  }
}
