package com.shashi.expensetracker.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.shashi.expensetracker.budget.BudgetsRoute
import com.shashi.expensetracker.expense.ExpensesRoute
import com.shashi.expensetracker.home.HomeRoute
import com.shashi.expensetracker.inbox.InboxRoute
import com.shashi.expensetracker.settings.SettingsRoute
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme

/** Cross-fade between tabs. Reduce motion swaps it for an instant change. */
private const val TAB_FADE_MILLIS = 200

/**
 * The root navigation host inside [AppShell].
 *
 * The root graph holds the nested [MainGraph] with the five tabs, Home first. This is the sign-in
 * gate seam: the account epic adds a sign-in graph next to [MainGraph] and passes it as
 * [startDestination]. [inboxBadgeCount] feeds the Inbox nav badge (hidden at 0).
 */
@Composable
fun AppNavHost(
  modifier: Modifier = Modifier,
  startDestination: String = MainGraph,
  inboxBadgeCount: Int = 0,
  navController: NavHostController = rememberNavController(),
) {
  val reduceMotion = ExpenseTrackerTheme.reduceMotion
  AppShell(
    navController = navController,
    inboxBadgeCount = inboxBadgeCount,
    navVisibleBeforeFirstDestination = startDestination == MainGraph,
    modifier = modifier,
  ) { contentPadding ->
    // Screens read the padding through a stable State, so inset changes don't rebuild the graph.
    val padding = rememberUpdatedState(contentPadding)
    NavHost(
      navController = navController,
      startDestination = startDestination,
      enterTransition = {
        if (reduceMotion) EnterTransition.None else fadeIn(tween(TAB_FADE_MILLIS))
      },
      exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(TAB_FADE_MILLIS)) },
    ) {
      mainGraph(padding)
    }
  }
}

private fun NavGraphBuilder.mainGraph(padding: State<PaddingValues>) {
  navigation(startDestination = TopLevelDestination.Home.route, route = MainGraph) {
    composable(TopLevelDestination.Home.route) { HomeRoute(contentPadding = padding.value) }
    composable(TopLevelDestination.Expenses.route) { ExpensesRoute(contentPadding = padding.value) }
    composable(TopLevelDestination.Inbox.route) { InboxRoute(contentPadding = padding.value) }
    composable(TopLevelDestination.Budgets.route) { BudgetsRoute(contentPadding = padding.value) }
    composable(TopLevelDestination.Settings.route) { SettingsRoute(contentPadding = padding.value) }
  }
}
