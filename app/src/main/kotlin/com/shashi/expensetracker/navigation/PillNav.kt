package com.shashi.expensetracker.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.shashi.expensetracker.navigation.components.NavBadge
import com.shashi.expensetracker.navigation.components.NavItemHeight
import com.shashi.expensetracker.navigation.components.navActiveBrush
import com.shashi.expensetracker.navigation.components.navItemDescription
import com.shashi.expensetracker.navigation.components.navSizeAnimationSpec
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.PillShape
import com.shashi.expensetracker.ui.theme.glassSurface

/** Glass between the pill's edge and its items. */
private val PillNavPadding = 4.dp

/** Total height of the pill nav: one item row plus its padding. */
val PillNavHeight: Dp = NavItemHeight + PillNavPadding * 2

/**
 * The floating glass pill nav for compact widths (DESIGN.md components.pill-nav).
 *
 * The [selected] item is about twice the width of the others, filled with the gradient, and shows
 * icon plus label; the others show only an icon with a TalkBack label. Every item is at least 48dp
 * in both directions. [inboxBadgeCount] drives the Inbox badge, hidden at 0.
 */
@Composable
fun PillNav(
  selected: TopLevelDestination?,
  onSelect: (TopLevelDestination) -> Unit,
  modifier: Modifier = Modifier,
  inboxBadgeCount: Int = 0,
) {
  BoxWithConstraints(
    modifier =
      modifier
        .testTag("pill_nav")
        .fillMaxWidth()
        .glassSurface(PillShape, navStyle = true)
        .padding(PillNavPadding)
  ) {
    val destinations = TopLevelDestination.entries
    // The active item takes two shares and each inactive item one, but never below 48dp.
    val inactiveWidth = max(NavItemHeight, maxWidth / (destinations.size + 1))
    val activeWidth = max(inactiveWidth, maxWidth - inactiveWidth * (destinations.size - 1))
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
      destinations.forEach { destination ->
        val isSelected = destination == selected
        PillNavItem(
          destination = destination,
          selected = isSelected,
          width = if (isSelected) activeWidth else inactiveWidth,
          badgeCount = if (destination == TopLevelDestination.Inbox) inboxBadgeCount else 0,
          onClick = { onSelect(destination) },
        )
      }
    }
  }
}

@Composable
private fun PillNavItem(
  destination: TopLevelDestination,
  selected: Boolean,
  width: Dp,
  badgeCount: Int,
  onClick: () -> Unit,
) {
  val animatedWidth by animateDpAsState(width, navSizeAnimationSpec(), label = "pillNavItemWidth")
  val label = stringResource(destination.label)
  val description = navItemDescription(label, badgeCount)
  val contentColor =
    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
  val interactionSource = remember { MutableInteractionSource() }
  // The outer box is the touch target and semantics node; the inner box draws the clipped pill and
  // ripple, so the badge can sit on top without being clipped.
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.width(animatedWidth)
        .height(NavItemHeight)
        .testTag(destination.testTag)
        .selectable(
          selected = selected,
          onClick = onClick,
          role = Role.Tab,
          interactionSource = interactionSource,
          indication = null,
        )
        .semantics { contentDescription = description },
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(PillShape)
          .then(if (selected) Modifier.background(navActiveBrush()) else Modifier)
          .indication(interactionSource, ripple())
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
      Box {
        Icon(
          painter = painterResource(destination.icon),
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(24.dp),
        )
        NavBadge(count = badgeCount, modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp))
      }
      if (selected) {
        Spacer(Modifier.width(6.dp))
        Text(
          text = label,
          color = contentColor,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Preview(widthDp = 360)
@Composable
private fun PillNavPreview() {
  ExpenseTrackerTheme {
    PillNav(selected = TopLevelDestination.Home, onSelect = {}, inboxBadgeCount = 3)
  }
}
