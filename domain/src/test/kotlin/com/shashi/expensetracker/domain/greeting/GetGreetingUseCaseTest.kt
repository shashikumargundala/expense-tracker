package com.shashi.expensetracker.domain.greeting

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GetGreetingUseCaseTest {

  private class FakeGreetingGateway(private val fetch: () -> String) : GreetingGateway {
    override suspend fun fetchName(): String = fetch()
  }

  @Test
  fun `returns greeting when gateway returns a name`() = runTest {
    val useCase = GetGreetingUseCase(FakeGreetingGateway { "Shashi" })

    assertEquals(GreetingResult.Success("Hello, Shashi!"), useCase())
  }

  @Test
  fun `trims a padded name in the greeting`() = runTest {
    val useCase = GetGreetingUseCase(FakeGreetingGateway { "  Shashi \n" })

    assertEquals(GreetingResult.Success("Hello, Shashi!"), useCase())
  }

  @Test
  fun `returns NameUnavailable when gateway throws`() = runTest {
    val useCase = GetGreetingUseCase(FakeGreetingGateway { error("offline") })

    assertEquals(GreetingResult.NameUnavailable, useCase())
  }

  @Test
  fun `returns NameUnavailable when gateway returns a blank name`() = runTest {
    val useCase = GetGreetingUseCase(FakeGreetingGateway { "   " })

    assertEquals(GreetingResult.NameUnavailable, useCase())
  }

  @Test
  fun `rethrows cancellation instead of returning NameUnavailable`() = runTest {
    val useCase = GetGreetingUseCase(FakeGreetingGateway { throw CancellationException("cancelled") })

    try {
      useCase()
      fail("Expected CancellationException")
    } catch (e: CancellationException) {
      assertEquals("cancelled", e.message)
    }
  }
}
