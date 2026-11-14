package com.shashi.expensetracker.expense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.R
import com.shashi.expensetracker.expense.components.ExpenseFormSheet
import com.shashi.expensetracker.expense.components.ExpenseRow
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/** The FAB's height plus its margins. */
private val FAB_CLEARANCE = 88.dp

/** The Expenses tab, wired to its ViewModel. [contentPadding] keeps content clear of bars and nav. */
@Composable
fun ExpensesRoute(contentPadding: PaddingValues, viewModel: ExpensesViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ExpensesScreen(uiState = uiState, onAdd = viewModel::add, contentPadding = contentPadding)
}

/**
 * The minimal Expenses tab (the tracer): the title, the mirror's Expenses newest first with a
 * "Waiting to sync" chip on pending rows, and an add FAB (`expenses_add`) that opens the minimal
 * form. The designed list, form, money formatter and period filter replace it later in epic 3.
 */
@Composable
fun ExpensesScreen(
  uiState: ExpensesUiState,
  onAdd: (amountPaise: Long, category: String, description: String) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  var formOpen by rememberSaveable { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxSize().testTag("screen_expenses")) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = contentPadding,
    ) {
      item(key = "title") {
        Text(
          text = stringResource(R.string.tab_expenses),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4)
              .semantics { heading() },
        )
      }
      if (uiState.saveFailed) {
        item(key = "error") {
          Text(
            text = stringResource(R.string.expenses_save_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = Spacing.marginMobile).testTag("expenses_error"),
          )
        }
      }
      if (uiState.loaded && uiState.expenses.isEmpty()) {
        item(key = "empty") {
          Text(
            text = stringResource(R.string.expenses_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.marginMobile),
          )
        }
      }
      items(uiState.expenses, key = { it.expense.id }) { item ->
        ExpenseRow(item = item, modifier = Modifier.padding(horizontal = Spacing.marginMobile))
      }
      // Keeps the last row clear of the FAB.
      item(key = "fab_space") { Spacer(Modifier.height(FAB_CLEARANCE)) }
    }
    FloatingActionButton(
      onClick = { formOpen = true },
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(contentPadding)
          .padding(Spacing.marginMobile)
          .testTag("expenses_add"),
    ) {
      Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.expenses_add))
    }
  }
  if (formOpen) {
    ExpenseFormSheet(
      categories = uiState.categories,
      onSave = { amountPaise, category, description ->
        onAdd(amountPaise, category, description)
        formOpen = false
      },
      onDismiss = { formOpen = false },
    )
  }
}

@Preview
@Composable
private fun ExpensesScreenPreview() {
  ExpenseTrackerTheme { ExpensesScreen(uiState = ExpensesUiState(loaded = true), onAdd = { _, _, _ -> }) }
}
