package com.shashi.expensetracker.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.R
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/** The Settings tab, wired to its ViewModel. [contentPadding] keeps content clear of bars and nav. */
@Composable
fun SettingsRoute(contentPadding: PaddingValues, viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  SettingsScreen(uiState = uiState, contentPadding = contentPadding)
}

/** Settings placeholder: the title in the screen-title style. */
@Suppress("UNUSED_PARAMETER") // uiState carries no content until the Settings epic.
@Composable
fun SettingsScreen(
  uiState: SettingsUiState,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("screen_settings")
        .padding(contentPadding)
        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4)
  ) {
    Text(
      text = stringResource(R.string.tab_settings),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.semantics { heading() },
    )
  }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
  ExpenseTrackerTheme { SettingsScreen(uiState = SettingsUiState()) }
}
