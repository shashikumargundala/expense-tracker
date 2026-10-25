package com.shashi.expensetracker.domain.greeting

/** Outside source of the name to greet. Implemented in `:data`. */
interface GreetingGateway {
  /** Returns the name to greet. May throw if the source is unavailable. */
  suspend fun fetchName(): String
}
