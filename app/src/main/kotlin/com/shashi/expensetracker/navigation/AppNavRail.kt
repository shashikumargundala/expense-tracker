package com.shashi.expensetracker.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.shashi.expensetracker.navigation.components.NavBadge
import com.shashi.expensetracker.navigation.components.NavItemHeight
import com.shashi.expensetracker.navigation.components.navActiveBrush
import com.shashi.expensetracker.navigation.components.navItemDescription
import com.shashi.expensetracker.navigation.components.navSizeAnimationSpec
import com.shashi.expensetracker.navigation.components.showActiveNavLabel
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.PillShape
import com.shashi.expensetracker.ui.theme.glassSurface

/** Glass between the rail's edge and its items. */
private val NavRailPadding = 8.dp

/** Width of each rail item; the active pill fills it. */
private val NavRailItemWidth = 80.dp

/** Horizontal padding around the active rail item's label. */
private val NavRailLabelPadding = 4.dp

/** The active rail item is taller so its label fits under the icon. */
private val NavRailActiveItemHeight = 56.dp

/** Total width of the rail, so the shell can keep content clear of it. */
val NavRailWidth: Dp = NavRailItemWidth + NavRailPadding * 2

/**
 * The navigation rail for widths of 600dp and more (DESIGN.md components.pill-nav). A vertical
 * floating glass pill with the same five items, gradient active item and Inbox badge as [PillNav].
 * Like the pill, the active item shows its icon only above 1.3x font or when its label doesn't fit.
 */
@Composable
fun AppNavRail(
  selected: TopLevelDestination?,
  onSelect: (TopLevelDestination) -> Unit,
  modifier: Modifier = Modifier,
  inboxBadgeCount: Int = 0,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .testTag("nav_rail")
        .width(NavRailWidth)
        .glassSurface(PillShape, navStyle = true)
        .padding(vertical = NavRailPadding * 2, horizontal = NavRailPadding)
        .selectableGroup(),
  ) {
    TopLevelDestination.entries.forEach { destination ->
      NavRailItem(
        destination = destination,
        selected = destination == selected,
        badgeCount = if (destination == TopLevelDestination.Inbox) inboxBadgeCount else 0,
        onClick = { onSelect(destination) },
      )
    }
  }
}

@Composable
private fun NavRailItem(
  destination: TopLevelDestination,
  selected: Boolean,
  badgeCount: Int,
  onClick: () -> Unit,
) {
  val height by
    animateDpAsState(
      if (selected) NavRailActiveItemHeight else NavItemHeight,
      navSizeAnimationSpec(),
      label = "navRailItemHeight",
    )
  val label = stringResource(destination.label)
  val description = navItemDescription(label, badgeCount)
  val contentColor =
    if (selected) ExpenseTrackerTheme.extendedColors.onGradient
    else MaterialTheme.colorScheme.onSurfaceVariant
  val showLabel = selected && showActiveNavLabel(label, NavRailItemWidth - NavRailLabelPadding * 2)
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.width(NavRailItemWidth)
        .height(height)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box {
        Icon(
          painter = painterResource(destination.icon),
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(24.dp),
        )
        NavBadge(count = badgeCount, modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp))
      }
      if (showLabel) {
        Text(
          text = label,
          color = contentColor,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = NavRailLabelPadding),
        )
      }
    }
  }
}

@Preview(heightDp = 420)
@Composable
private fun AppNavRailPreview() {
  ExpenseTrackerTheme {
    AppNavRail(selected = TopLevelDestination.Home, onSelect = {}, inboxBadgeCount = 3)
  }
}
