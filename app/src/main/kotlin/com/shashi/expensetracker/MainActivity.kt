package com.shashi.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.navigation.AppNavHost
import com.shashi.expensetracker.navigation.MainGraph
import com.shashi.expensetracker.navigation.SignInGraph
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The navigation host only: edge-to-edge, the theme, then [AppNavHost], started at Home when a
 * session is stored and at Sign-in otherwise. Until the session is read it draws only the canvas,
 * so neither screen flashes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val startViewModel: StartViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      ExpenseTrackerTheme {
        val start by startViewModel.uiState.collectAsStateWithLifecycle()
        when (start) {
          StartUiState.Loading ->
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
          StartUiState.SignedIn -> AppNavHost(startDestination = MainGraph)
          StartUiState.SignedOut -> AppNavHost(startDestination = SignInGraph)
        }
      }
    }
  }
}
