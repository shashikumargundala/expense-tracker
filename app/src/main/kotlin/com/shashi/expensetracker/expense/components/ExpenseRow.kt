package com.shashi.expensetracker.expense.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.shashi.expensetracker.R
import com.shashi.expensetracker.domain.expense.ExpenseListItem
import com.shashi.expensetracker.ui.theme.Spacing
import java.time.format.DateTimeFormatter

private val rowDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * One minimal Expense row: amount, Category, description and date, with a "Waiting to sync" chip
 * (`expense_pending`) while the Expense has an outbox entry. The designed row replaces it later.
 */
@Composable
internal fun ExpenseRow(item: ExpenseListItem, modifier: Modifier = Modifier) {
  val expense = item.expense
  Row(
    modifier = modifier.fillMaxWidth().testTag("expense_row").padding(vertical = Spacing.s2),
    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        text = expense.category,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (expense.description.isNotEmpty()) {
        Text(
          text = expense.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = expense.date.format(rowDate),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = formatPaise(expense.amountPaise),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (item.pending) PendingChip()
    }
  }
}

@Composable
private fun PendingChip() {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.padding(top = Spacing.s1).testTag("expense_pending"),
  ) {
    Text(
      text = stringResource(R.string.expenses_waiting_to_sync),
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = Spacing.s2, vertical = Spacing.s1),
    )
  }
}
