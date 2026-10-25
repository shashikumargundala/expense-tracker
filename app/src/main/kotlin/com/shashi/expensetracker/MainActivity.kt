package com.shashi.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shashi.expensetracker.navigation.AppNavHost
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/** The navigation host only: edge-to-edge, the theme, then [AppNavHost]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent { ExpenseTrackerTheme { AppNavHost() } }
  }
}
