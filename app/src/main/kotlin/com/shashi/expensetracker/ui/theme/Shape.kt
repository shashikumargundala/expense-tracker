package com.shashi.expensetracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** DESIGN.md `rounded` scale. Every shape is round; there are no sharp corners. */
object Radius {
  /** Fallback radius for pills that wrap to several lines at large font sizes. */
  val sm = 12.dp
  /** Form fields. */
  val md = 18.dp
  /** Glass tiles and rows. */
  val lg = 24.dp
  /** Hero chart, sheet top corners, dialogs. */
  val xl = 28.dp
}

/** `rounded.full` (9999px): pills and circles. */
val PillShape = RoundedCornerShape(percent = 50)

// extraSmall has no DESIGN.md token; it uses `sm` so nothing renders with sharp corners.
val ExpenseTrackerShapes =
  Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
  )
