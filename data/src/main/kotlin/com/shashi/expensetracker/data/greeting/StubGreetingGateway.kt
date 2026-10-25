package com.shashi.expensetracker.data.greeting

import com.shashi.expensetracker.domain.greeting.GreetingGateway
import javax.inject.Inject

/** Tracer stub until real data sources exist. Story 1.5 removes the greeting package. */
class StubGreetingGateway @Inject constructor() : GreetingGateway {
  override suspend fun fetchName(): String = "Expense Tracker"
}
