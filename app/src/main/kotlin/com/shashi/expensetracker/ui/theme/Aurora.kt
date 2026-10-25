package com.shashi.expensetracker.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The aurora background: three soft radial glows (aurora-1 top-left, aurora-2 top-right, aurora-3
 * mid) over the canvas. Plain gradients, so it renders on every API level. Decoration only.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
  val canvas = MaterialTheme.colorScheme.background
  val colors = LocalExtendedColors.current
  Box(
    modifier =
      modifier.drawBehind {
        drawRect(canvas)
        val glow = size.minDimension * 0.75f
        drawGlow(colors.aurora1, Offset(0f, 0f), glow)
        drawGlow(colors.aurora2, Offset(size.width, 0f), glow)
        drawGlow(colors.aurora3, Offset(size.width * 0.5f, size.height * 0.45f), glow)
      },
    content = content,
  )
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float) {
  if (radius <= 0f) return
  drawCircle(
    brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius),
    radius = radius,
    center = center,
  )
}
