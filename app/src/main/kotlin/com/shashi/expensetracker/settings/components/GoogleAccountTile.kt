package com.shashi.expensetracker.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shashi.expensetracker.R
import com.shashi.expensetracker.navigation.components.ActiveNavGradientEnd
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.PillShape
import com.shashi.expensetracker.ui.theme.Radius
import com.shashi.expensetracker.ui.theme.Spacing
import com.shashi.expensetracker.ui.theme.glassSurface

// Badge and icon sizes are in sp, so they follow the font scale like the mockup's em sizes.

/** Diameter of the avatar and the Sheet orb (mockup 2.6em). */
private val BadgeSize = 40.sp

/** The Sheet icon in its orb (mockup 1.2em). */
private val OrbIconSize = 20.sp

/** The external-link icon after Open my Sheet (mockup 1.1em). */
private val LinkIconSize = 18.sp

/** Space between a badge and its text. */
private val BadgeGap = Spacing.s3

/** [size] in dp at the current font scale. */
@Composable
@ReadOnlyComposable
private fun scaled(size: TextUnit): Dp = with(LocalDensity.current) { size.toDp() }

/** The divider and Open my Sheet line up with the row text, past the badge (mockup `.div`). */
@Composable
@ReadOnlyComposable
private fun textInset(): Dp = scaled(BadgeSize) + BadgeGap

/** Minimum touch target of Open my Sheet. */
private val ActionMinHeight = 48.dp

/**
 * The Settings "Google account" glass tile (DESIGN.md Settings screen, `key-settings.html`
 * State 1): the account row (gradient avatar with [initial], [email], "Signed in with Google"),
 * then, when [sheetId] is set, an inset divider, the Expense Sheet row and the Open my Sheet text
 * action, which calls [onOpenSheet] with the ID. TalkBack reads each row as one item and Open my
 * Sheet as a button.
 */
@Composable
fun GoogleAccountTile(
  email: String,
  initial: String,
  sheetId: String?,
  onOpenSheet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("settings_account")
        .glassSurface(RoundedCornerShape(Radius.lg))
        .padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s3, bottom = Spacing.s1)
  ) {
    TileRow(
      badge = { Avatar(initial) },
      title = email,
      subtitle = stringResource(R.string.settings_signed_in_with_google),
      modifier = Modifier.padding(bottom = if (sheetId == null) Spacing.s2 else 0.dp),
    )
    if (sheetId != null) {
      HorizontalDivider(
        modifier = Modifier.padding(start = textInset(), top = Spacing.s3, bottom = Spacing.s3),
        color = MaterialTheme.colorScheme.outlineVariant,
      )
      TileRow(
        badge = { SheetOrb() },
        title = stringResource(R.string.settings_expense_sheet),
        subtitle = stringResource(R.string.settings_in_your_drive),
      )
      OpenSheetAction(onClick = { onOpenSheet(sheetId) })
    }
  }
}

@Composable
private fun TileRow(
  badge: @Composable () -> Unit,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    horizontalArrangement = Arrangement.spacedBy(BadgeGap),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    badge()
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        // No maxLines: a long email wraps, and a word wider than the row breaks mid-word.
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** The initial on the brand gradient at 135°, hidden from TalkBack (the email says it all). */
@Composable
private fun Avatar(initial: String) {
  Box(
    modifier =
      Modifier.size(scaled(BadgeSize))
        .background(avatarBrush(), CircleShape)
        .clearAndSetSemantics {},
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = ExpenseTrackerTheme.extendedColors.onGradient,
      maxLines = 1,
    )
  }
}

/** Gradient-start to the text-safe pink, top-left to bottom-right (135°). */
@Composable
@ReadOnlyComposable
private fun avatarBrush(): Brush =
  Brush.linearGradient(
    colors = listOf(ExpenseTrackerTheme.extendedColors.gradientStart, ActiveNavGradientEnd),
    start = Offset.Zero,
    end = Offset.Infinite,
  )

@Composable
private fun SheetOrb() {
  Box(
    modifier =
      Modifier.size(scaled(BadgeSize))
        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_sheet),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(scaled(OrbIconSize)),
    )
  }
}

@Composable
private fun OpenSheetAction(onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.padding(start = textInset() - Spacing.s2)
        .testTag("settings_open_sheet")
        .heightIn(min = ActionMinHeight)
        .clip(PillShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = Spacing.s2),
    horizontalArrangement = Arrangement.spacedBy(Spacing.s1 + 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.settings_open_sheet),
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary,
    )
    Icon(
      painter = painterResource(R.drawable.ic_open_in_new),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(scaled(LinkIconSize)),
    )
  }
}

@Preview
@Composable
private fun GoogleAccountTilePreview() {
  ExpenseTrackerTheme {
    GoogleAccountTile(
      email = "shylock@gmail.com",
      initial = "S",
      sheetId = "sheet",
      onOpenSheet = {},
      modifier = Modifier.padding(Spacing.s4),
    )
  }
}
