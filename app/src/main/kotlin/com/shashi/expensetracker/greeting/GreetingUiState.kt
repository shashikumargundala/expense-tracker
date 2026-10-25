package com.shashi.expensetracker.greeting

/** State of [GreetingScreen]. [message] is null while the greeting is loading. */
data class GreetingUiState(val message: String? = null) {
  companion object {
    const val FALLBACK_MESSAGE = "Welcome to Expense Tracker"
  }
}
