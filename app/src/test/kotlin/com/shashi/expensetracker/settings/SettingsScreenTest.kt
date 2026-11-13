package com.shashi.expensetracker.settings

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// A plain Application keeps the Hilt graph out of this stateless screen test.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class SettingsScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val opened = mutableListOf<String>()

  private fun show(state: SettingsUiState) {
    composeRule.setContent {
      ExpenseTrackerTheme { SettingsScreen(uiState = state, onOpenSheet = { opened += it }) }
    }
  }

  @Test
  fun showsTheAccountWithInitialAndSheetRow() {
    show(SettingsUiState(loaded = true, email = "tester@example.com", sheetId = "test-sheet"))

    composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_account").assertIsDisplayed()
    composeRule.onNodeWithText("tester@example.com", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("T", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("Expense Sheet", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithTag("settings_open_sheet").assertIsDisplayed()
  }

  @Test
  fun readsTheAccountRowAsOneItem() {
    show(SettingsUiState(loaded = true, email = "tester@example.com", sheetId = "test-sheet"))

    composeRule
      .onNodeWithText("tester@example.com")
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Text,
          listOf(
            androidx.compose.ui.text.AnnotatedString("tester@example.com"),
            androidx.compose.ui.text.AnnotatedString("Signed in with Google"),
          ),
        )
      )
    composeRule
      .onNodeWithTag("settings_open_sheet")
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
  }

  @Test
  fun hidesTheSheetRowWithoutASheetId() {
    show(SettingsUiState(loaded = true, email = "tester@example.com", sheetId = null))

    composeRule.onNodeWithTag("settings_account").assertIsDisplayed()
    composeRule.onNodeWithText("Expense Sheet", useUnmergedTree = true).assertDoesNotExist()
    composeRule.onNodeWithTag("settings_open_sheet").assertDoesNotExist()
  }

  @Test
  fun showsOnlyTheTitleWhileLoading() {
    show(SettingsUiState())

    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_account").assertDoesNotExist()
  }

  @Test
  fun openMySheetPassesTheSheetId() {
    show(SettingsUiState(loaded = true, email = "tester@example.com", sheetId = "test-sheet"))

    composeRule.onNodeWithTag("settings_open_sheet").performClick()

    assertEquals(listOf("test-sheet"), opened)
  }
}
