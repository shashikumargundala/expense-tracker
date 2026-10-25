package com.shashi.expensetracker.home

/** State of [HomeScreen]. [greeting] is null while the greeting is loading. */
data class HomeUiState(val greeting: String? = null) {
  companion object {
    const val FALLBACK_GREETING = "Welcome to Expense Tracker"
  }
}
