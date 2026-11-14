package com.shashi.expensetracker.expense.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.shashi.expensetracker.R
import com.shashi.expensetracker.ui.theme.Spacing

/**
 * The minimal Expense form in a bottom sheet: amount, one Category chip, an optional description and
 * Save, which stays disabled until the amount is positive and a Category is chosen. The date is
 * "now", set by the use case. Typed values are transient UI state (AD-15). The designed form
 * replaces it later in epic 3.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ExpenseFormSheet(
  categories: List<String>,
  onSave: (amountPaise: Long, category: String, description: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var amount by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf<String?>(null) }
  var description by rememberSaveable { mutableStateOf("") }
  val amountPaise = parseAmountPaise(amount)
  val chosen = category?.takeIf { it in categories }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrimColor = MaterialTheme.colorScheme.scrim,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = Spacing.marginMobile, vertical = Spacing.s2),
      verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
      OutlinedTextField(
        value = amount,
        onValueChange = { amount = it },
        // A placeholder, not a label: the theme's bodyLarge (em) and bodySmall (sp) line heights
        // can't be interpolated, so an animated M3 label throws.
        placeholder = { Text(stringResource(R.string.expense_form_amount)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("expense_form_amount"),
      )
      Text(
        text = stringResource(R.string.expense_form_category),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (categories.isEmpty()) {
        Text(
          text = stringResource(R.string.expense_form_no_categories),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
      ) {
        categories.forEach { name ->
          FilterChip(
            selected = name == chosen,
            onClick = { category = name },
            label = { Text(name) },
            modifier = Modifier.testTag("expense_form_category"),
          )
        }
      }
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        placeholder = { Text(stringResource(R.string.expense_form_description)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("expense_form_description"),
      )
      Button(
        onClick = {
          if (amountPaise != null && chosen != null) onSave(amountPaise, chosen, description)
        },
        enabled = amountPaise != null && chosen != null,
        modifier = Modifier.fillMaxWidth().testTag("expense_form_save"),
      ) {
        Text(stringResource(R.string.expense_form_save))
      }
    }
  }
}
