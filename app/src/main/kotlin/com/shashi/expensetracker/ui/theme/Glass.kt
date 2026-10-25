package com.shashi.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The 1dp light edge every glass surface draws (DESIGN.md components.glass-tile). */
val GlassEdgeWidth = 1.dp

/**
 * Whether translucent glass is allowed. Real background blur needs `RenderEffect` (API 31+);
 * below that, glass must be opaque so text contrast holds (DESIGN.md Elevation & Depth).
 */
fun supportsBlur(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S

/** Fill for a glass surface: the translucent token on API 31+, opaque surfaceContainer below. */
fun glassFill(
  sdkInt: Int,
  navStyle: Boolean,
  extendedColors: ExtendedColors,
  surfaceContainer: Color,
): Color =
  when {
    !supportsBlur(sdkInt) -> surfaceContainer
    navStyle -> extendedColors.navGlass
    else -> extendedColors.glass
  }

/**
 * A frosted-glass surface: fill, 1dp edge and [shape] clip.
 *
 * Decision (2026-10-25): no backdrop-blur library. On API 31+ this is a translucent fill over the
 * aurora; content scrolling underneath isn't blurred. On API 24-30 it's opaque surfaceContainer with
 * the same edge and radius. [navStyle] uses the denser nav-glass token.
 */
fun Modifier.glassSurface(shape: Shape, navStyle: Boolean = false): Modifier = composed {
  val extendedColors = LocalExtendedColors.current
  val fill =
    glassFill(
      sdkInt = Build.VERSION.SDK_INT,
      navStyle = navStyle,
      extendedColors = extendedColors,
      surfaceContainer = MaterialTheme.colorScheme.surfaceContainer,
    )
  Modifier.clip(shape)
    .background(fill, shape)
    .border(GlassEdgeWidth, extendedColors.glassEdge, shape)
}
