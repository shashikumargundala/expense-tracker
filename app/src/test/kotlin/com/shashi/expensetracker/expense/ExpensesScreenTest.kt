package com.shashi.expensetracker.expense

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.MainActivity
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.ExpenseSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The tracer end to end through the real activity, Room mirror and sync coordinator, with the Sheet
 * faked: launch syncs the Categories in, an added Expense lists at once as "Waiting to sync", and
 * reopening the app pushes it to the Sheet and clears the chip.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class ExpensesScreenTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

  @Inject lateinit var sheet: FakeExpenseSheetGateway

  private var scenario: ActivityScenario<MainActivity>? = null

  @Before
  fun setUp() {
    hiltRule.inject()
    sheet.categories += Category("Food", "#F59E0B", "restaurant")
    sheet.categories += Category("Bills", "#3B82F6", "receipt_long")
  }

  @After
  fun tearDown() {
    scenario?.close()
  }

  private fun waitFor(matcher: androidx.compose.ui.test.SemanticsMatcher) =
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

  private fun openExpenses() {
    scenario = ActivityScenario.launch(MainActivity::class.java)
    waitFor(hasTestTag("screen_home"))
    composeRule.onNodeWithTag("nav_expenses").performClick()
    waitFor(hasTestTag("screen_expenses"))
  }

  private fun openFormWithCategories() {
    composeRule.onNodeWithTag("expenses_add").performClick()
    waitFor(hasText("Food"))
  }

  @Test
  fun saveStaysDisabledUntilAmountAndCategoryAreValid() {
    openExpenses()
    openFormWithCategories()

    composeRule.onNodeWithTag("expense_form_save").assertIsNotEnabled()
    composeRule.onNodeWithTag("expense_form_amount").performTextInput("0")
    composeRule.onNodeWithText("Food").performClick()
    composeRule.onNodeWithTag("expense_form_save").assertIsNotEnabled()

    composeRule.onNodeWithTag("expense_form_amount").performTextInput("5")
    composeRule.onNodeWithTag("expense_form_save").assertIsEnabled()
  }

  @Test
  fun anAddedExpenseListsAtOnceAsWaitingToSyncThenReopeningSyncsIt() {
    openExpenses()
    openFormWithCategories()

    composeRule.onNodeWithTag("expense_form_amount").performTextInput("120")
    composeRule.onNodeWithText("Food").performClick()
    composeRule.onNodeWithTag("expense_form_description").performTextInput("Chai")
    composeRule.onNodeWithTag("expense_form_save").performClick()

    waitFor(hasTestTag("expense_pending"))
    composeRule.onNodeWithTag("expense_row").assertIsDisplayed()
    composeRule.onNodeWithText("Chai", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("₹120.00", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("Waiting to sync", useUnmergedTree = true).assertIsDisplayed()
    // No sync trigger fires on add; the Sheet hasn't seen it yet.
    assertTrue(sheet.writes.isEmpty())

    // Reopen the app: the cold start's sync pushes it and the chip clears.
    scenario?.close()
    openExpenses()
    waitFor(hasTestTag("expense_row"))
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasTestTag("expense_pending"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
    val written = sheet.expenses.single()
    assertEquals(12_000L, written.amountPaise)
    assertEquals("Food", written.category)
    assertEquals("Chai", written.description)
    assertEquals(ExpenseSource.App, written.source)
  }
}
