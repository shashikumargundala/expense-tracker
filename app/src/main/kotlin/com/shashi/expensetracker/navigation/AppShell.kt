package com.shashi.expensetracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shashi.expensetracker.ui.theme.AuroraBackground
import com.shashi.expensetracker.ui.theme.Spacing

/** At this width and wider the rail replaces the pill nav (DESIGN.md Layout & Spacing). */
val NavRailMinWidth = 600.dp

/** Margin between the floating nav and the screen edge. */
private val NavMargin = Spacing.s4

/**
 * The app chrome around [content]: the aurora background, and the [PillNav] below 600dp or the
 * [AppNavRail] at 600dp and wider, shown only while a tab of the main graph is on screen.
 *
 * It owns tab navigation: every tab pops back to Home, so Back from any other tab lands on Home and
 * Back from Home leaves to the system; each tab keeps its own state. [content] gets padding that
 * keeps it clear of the system bars (`safeDrawing`) and of the floating nav.
 * [navVisibleBeforeFirstDestination] decides the first frame, before the back stack exists.
 */
@Composable
fun AppShell(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  inboxBadgeCount: Int = 0,
  navVisibleBeforeFirstDestination: Boolean = true,
  content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val destination = backStackEntry?.destination
  val navVisible = destination?.isInGraph(MainGraph) ?: navVisibleBeforeFirstDestination
  val selected = TopLevelDestination.entries.firstOrNull { destination?.isInGraph(it.route) == true }
  val onSelect: (TopLevelDestination) -> Unit = { tab ->
    navController.navigate(tab.route) {
      popUpTo(TopLevelDestination.Home.route) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
    AuroraBackground(Modifier.fillMaxSize()) {
      BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= NavRailMinWidth
        val contentPadding =
          shellContentPadding(
            safeDrawing = WindowInsets.safeDrawing.asPaddingValues(),
            navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            layoutDirection = LocalLayoutDirection.current,
            nav = if (!navVisible) ShellNav.None else if (wide) ShellNav.Rail else ShellNav.Pill,
          )
        // Content always sits first, at the same call site, so a width change never recreates it.
        Box(Modifier.fillMaxSize()) { content(contentPadding) }
        if (navVisible && wide) {
          AppNavRail(
            selected = selected,
            onSelect = onSelect,
            inboxBadgeCount = inboxBadgeCount,
            modifier =
              Modifier.align(Alignment.CenterStart)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                .padding(start = NavMargin),
          )
        } else if (navVisible) {
          PillNav(
            selected = selected,
            onSelect = onSelect,
            inboxBadgeCount = inboxBadgeCount,
            modifier =
              Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(
                  WindowInsets.navigationBars.union(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                )
                .padding(NavMargin),
          )
        }
      }
    }
  }
}

private enum class ShellNav {
  None,
  Pill,
  Rail,
}

/** `safeDrawing` padding, widened on the side where the floating nav sits so content clears it. */
private fun shellContentPadding(
  safeDrawing: PaddingValues,
  navigationBarsBottom: Dp,
  layoutDirection: LayoutDirection,
  nav: ShellNav,
): PaddingValues {
  val start = safeDrawing.calculateStartPadding(layoutDirection)
  val top = safeDrawing.calculateTopPadding()
  val end = safeDrawing.calculateEndPadding(layoutDirection)
  val bottom = safeDrawing.calculateBottomPadding()
  return when (nav) {
    ShellNav.None -> PaddingValues(start = start, top = top, end = end, bottom = bottom)
    ShellNav.Pill ->
      PaddingValues(
        start = start,
        top = top,
        end = end,
        bottom = max(bottom, navigationBarsBottom + NavMargin + PillNavHeight + Spacing.rowGap),
      )
    ShellNav.Rail ->
      PaddingValues(start = start + NavMargin + NavRailWidth, top = top, end = end, bottom = bottom)
  }
}

private fun NavDestination.isInGraph(route: String): Boolean = hierarchy.any { it.route == route }
