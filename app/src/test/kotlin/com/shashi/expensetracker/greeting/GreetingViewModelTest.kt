package com.shashi.expensetracker.greeting

import com.shashi.expensetracker.domain.greeting.GetGreetingUseCase
import com.shashi.expensetracker.domain.greeting.GreetingGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingViewModelTest {

  private class FakeGreetingGateway(private val fetch: () -> String) : GreetingGateway {
    override suspend fun fetchName(): String = fetch()
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `shows greeting when gateway returns a name`() {
    val viewModel = GreetingViewModel(GetGreetingUseCase(FakeGreetingGateway { "Shashi" }))

    assertEquals(GreetingUiState(message = "Hello, Shashi!"), viewModel.uiState.value)
  }

  @Test
  fun `shows fallback message when gateway fails`() {
    val viewModel = GreetingViewModel(GetGreetingUseCase(FakeGreetingGateway { error("offline") }))

    assertEquals(
      GreetingUiState(message = GreetingUiState.FALLBACK_MESSAGE),
      viewModel.uiState.value,
    )
  }
}
