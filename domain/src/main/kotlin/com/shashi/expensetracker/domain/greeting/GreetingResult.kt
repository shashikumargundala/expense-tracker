package com.shashi.expensetracker.domain.greeting

/** Result of [GetGreetingUseCase]: a greeting or a named failure. */
sealed interface GreetingResult {
  data class Success(val greeting: String) : GreetingResult

  /** The gateway failed or returned no usable name. */
  data object NameUnavailable : GreetingResult
}
