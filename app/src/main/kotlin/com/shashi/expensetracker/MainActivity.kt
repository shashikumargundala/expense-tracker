package com.shashi.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.greeting.GreetingScreen
import com.shashi.expensetracker.greeting.GreetingViewModel
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val greetingViewModel: GreetingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ExpenseTrackerTheme {
        val uiState = greetingViewModel.uiState.collectAsStateWithLifecycle()
        GreetingScreen(uiState = uiState.value)
      }
    }
  }
}
