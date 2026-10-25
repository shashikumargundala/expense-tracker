package com.shashi.expensetracker.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * True when the system "Remove animations" setting is on. Every animation reads this and swaps
 * motion for an instant change. There's no in-app toggle (EXPERIENCE.md Motion).
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads `Settings.Global.ANIMATOR_DURATION_SCALE`; 0 means reduce motion. Unreadable → false. */
fun readReduceMotion(context: Context): Boolean =
  try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  } catch (_: Exception) {
    false
  }

/** The system reduce-motion setting, re-read each time the lifecycle resumes. */
@Composable
fun rememberReduceMotion(): Boolean {
  val context = LocalContext.current
  var reduceMotion by remember(context) { mutableStateOf(readReduceMotion(context)) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { reduceMotion = readReduceMotion(context) }
  return reduceMotion
}
