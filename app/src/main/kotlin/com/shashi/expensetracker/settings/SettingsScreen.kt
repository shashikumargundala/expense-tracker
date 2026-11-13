package com.shashi.expensetracker.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.R
import com.shashi.expensetracker.settings.components.GoogleAccountTile
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/**
 * The Settings tab, wired to its ViewModel. [contentPadding] keeps content clear of bars and nav.
 * Open my Sheet launches the Sheet in the Google Sheets app ([openSheetInSheetsApp]).
 */
@Composable
fun SettingsRoute(contentPadding: PaddingValues, viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  SettingsScreen(
    uiState = uiState,
    onOpenSheet = { sheetId -> openSheetInSheetsApp(context, sheetId) },
    contentPadding = contentPadding,
  )
}

/**
 * Settings (DESIGN.md Settings screen): the title, then the "Google account" section with
 * [GoogleAccountTile]. Until the session loads, only the title shows. Preferences and Sign out come
 * in epic 3.
 */
@Composable
fun SettingsScreen(
  uiState: SettingsUiState,
  onOpenSheet: (String) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("screen_settings"),
    contentPadding = contentPadding,
  ) {
    item(key = "title") {
      Text(
        text = stringResource(R.string.tab_settings),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
          Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4)
            .semantics { heading() },
      )
    }
    val email = uiState.email
    if (uiState.loaded && email != null) {
      item(key = "account_header") {
        Text(
          text = stringResource(R.string.settings_google_account),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.padding(
                start = Spacing.marginMobile + Spacing.s1,
                end = Spacing.marginMobile,
                bottom = Spacing.s2,
              )
              .semantics { heading() },
        )
      }
      item(key = "account_tile") {
        GoogleAccountTile(
          email = email,
          initial = uiState.initial,
          sheetId = uiState.sheetId,
          onOpenSheet = onOpenSheet,
          modifier = Modifier.padding(horizontal = Spacing.marginMobile),
        )
      }
    }
  }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
  ExpenseTrackerTheme {
    SettingsScreen(
      uiState = SettingsUiState(loaded = true, email = "shylock@gmail.com", sheetId = "sheet"),
      onOpenSheet = {},
    )
  }
}
