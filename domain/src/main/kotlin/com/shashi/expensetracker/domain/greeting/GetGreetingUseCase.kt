package com.shashi.expensetracker.domain.greeting

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Builds the greeting from the [GreetingGateway]. Never throws to the caller. */
class GetGreetingUseCase @Inject constructor(private val gateway: GreetingGateway) {
  suspend operator fun invoke(): GreetingResult {
    val name =
      try {
        gateway.fetchName().trim()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        return GreetingResult.NameUnavailable
      }
    return if (name.isEmpty()) GreetingResult.NameUnavailable
    else GreetingResult.Success("Hello, $name!")
  }
}
