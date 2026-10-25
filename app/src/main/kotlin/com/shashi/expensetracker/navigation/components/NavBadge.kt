package com.shashi.expensetracker.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shashi.expensetracker.R
import com.shashi.expensetracker.ui.theme.PillShape

/** Counts above this show as "99+". TalkBack still reads the exact count. */
private const val MAX_SHOWN_COUNT = 99

/**
 * The Inbox count badge shared by the pill nav and the rail (DESIGN.md components.pill-nav.badge):
 * `{colors.exceeded}` with on-gradient text. Draws nothing when [count] is 0 or less. The badge adds
 * no semantics itself; its nav item speaks it through [navItemDescription].
 */
@Composable
fun NavBadge(count: Int, modifier: Modifier = Modifier) {
  if (count <= 0) return
  val shown = if (count > MAX_SHOWN_COUNT) "$MAX_SHOWN_COUNT+" else count.toString()
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .testTag("nav_inbox_badge")
        .clearAndSetSemantics {}
        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
        .background(MaterialTheme.colorScheme.error, PillShape)
        .padding(horizontal = 4.dp),
  ) {
    Text(
      text = shown,
      // onError is on-gradient (Theme.kt), the badge text color DESIGN.md names.
      color = MaterialTheme.colorScheme.onError,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    )
  }
}

/** The one TalkBack label of a nav item: its [label], plus ", N new" while its badge shows. */
@Composable
fun navItemDescription(label: String, badgeCount: Int): String =
  if (badgeCount <= 0) label
  else "$label, ${pluralStringResource(R.plurals.nav_badge_new, badgeCount, badgeCount)}"
