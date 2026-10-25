package com.shashi.expensetracker.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shashi.expensetracker.R

/**
 * Route of the nested graph that holds the five tabs. The account epic adds a sign-in graph next to
 * it and passes the start destination to [AppNavHost].
 */
const val MainGraph = "main"

/**
 * The five tabs, in tab order (EXPERIENCE.md Information Architecture). Home is the start tab.
 * Route strings live only here; screens never see the `NavController`.
 */
enum class TopLevelDestination(
  val route: String,
  @StringRes val label: Int,
  @DrawableRes val icon: Int,
  val testTag: String,
) {
  Home("home", R.string.tab_home, R.drawable.ic_nav_home, "nav_home"),
  Expenses("expenses", R.string.tab_expenses, R.drawable.ic_nav_list, "nav_expenses"),
  Inbox("inbox", R.string.tab_inbox, R.drawable.ic_nav_inbox, "nav_inbox"),
  Budgets("budgets", R.string.tab_budgets, R.drawable.ic_nav_pie_chart, "nav_budgets"),
  Settings("settings", R.string.tab_settings, R.drawable.ic_nav_tune, "nav_settings"),
}
