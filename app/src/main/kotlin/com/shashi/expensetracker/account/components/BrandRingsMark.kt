package com.shashi.expensetracker.account.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme

/** The mark's reference size; its strokes (12dp outer, 6dp inner) scale with the actual size. */
val BrandMarkReferenceSize = 112.dp

private const val SegmentCount = 5
private const val SegmentGapDegrees = 2f
private const val SegmentSweepDegrees = 360f / SegmentCount - SegmentGapDegrees

// Proportions of the mark's width, from DESIGN.md hero-chart and mockups/key-signin.html.
private const val OuterStrokeRatio = 12f / 112f
private const val InnerStrokeRatio = 6f / 112f
private const val InnerDiameterRatio = 0.70f
private const val InnerSweepFraction = 0.75f
private const val CoreDiameterRatio = 0.31f
private const val HaloWidthRatio = 0.042f
private const val GlowRadiusRatio = 0.29f

/**
 * Sign-in's static, decorative brand mark: five Category-colored outer segments (butt caps, 2°
 * gaps) on the ring track, an inner gradient orbit, and a glowing gradient core in a glass halo. It
 * mirrors the hero chart's geometry but is Sign-in's own drawing. It has no semantics, so TalkBack
 * skips it. The caller sets its size; the strokes scale with it.
 */
@Composable
fun BrandRingsMark(modifier: Modifier = Modifier) {
  val colors = ExpenseTrackerTheme.extendedColors
  val track = MaterialTheme.colorScheme.surfaceContainerHighest
  val segments =
    listOf(colors.categoryFood, colors.categoryShopping, colors.categoryBills, colors.categoryTransport, colors.categoryFun)
  Canvas(modifier) {
    val side = size.minDimension
    val outerStroke = side * OuterStrokeRatio
    drawRing(diameter = side - outerStroke, stroke = outerStroke, color = track)
    segments.forEachIndexed { index, color ->
      drawRingArc(
        diameter = side - outerStroke,
        stroke = outerStroke,
        startFromTop = index * (SegmentSweepDegrees + SegmentGapDegrees),
        sweep = SegmentSweepDegrees,
        brush = null,
        color = color,
      )
    }

    val innerStroke = side * InnerStrokeRatio
    val innerDiameter = side * InnerDiameterRatio - innerStroke
    drawRing(diameter = innerDiameter, stroke = innerStroke, color = track)
    rotate(-90f) {
      drawRingArc(
        diameter = innerDiameter,
        stroke = innerStroke,
        startFromTop = 90f,
        sweep = 360f * InnerSweepFraction,
        brush =
          Brush.sweepGradient(
            0f to colors.gradientStart,
            InnerSweepFraction to colors.gradientEnd,
            1f to colors.gradientEnd,
            center = center,
          ),
        color = Color.Unspecified,
      )
    }

    val coreRadius = side * CoreDiameterRatio / 2f
    drawCircle(
      brush = Brush.radialGradient(listOf(colors.gradientEnd.copy(alpha = 0.55f), Color.Transparent), center, side * GlowRadiusRatio),
      radius = side * GlowRadiusRatio,
    )
    drawCircle(color = colors.glass, radius = coreRadius + side * HaloWidthRatio)
    val highlight = Offset(center.x - coreRadius * 0.4f, center.y - coreRadius * 0.4f)
    drawCircle(
      brush = Brush.radialGradient(listOf(colors.gradientEnd, colors.gradientStart), highlight, coreRadius * 1.6f),
      radius = coreRadius,
    )
  }
}

/**
 * Sign-in's "setting up" ring: a gradient arc over the ring track (6dp stroke at 48dp), turning
 * once a second, or still when reduce motion is on. Decorative; the caller's text carries meaning.
 */
@Composable
fun SignInProgressRing(modifier: Modifier = Modifier, size: Dp = 48.dp) {
  val colors = ExpenseTrackerTheme.extendedColors
  val track = MaterialTheme.colorScheme.surfaceContainerHighest
  val rotation =
    if (ExpenseTrackerTheme.reduceMotion) {
      0f
    } else {
      val transition = rememberInfiniteTransition(label = "signInProgress")
      transition
        .animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec = infiniteRepeatable(tween(durationMillis = 1_000, easing = LinearEasing), RepeatMode.Restart),
          label = "signInProgressRotation",
        )
        .value
    }
  Canvas(modifier.size(size)) {
    val stroke = this.size.minDimension / 8f
    val diameter = this.size.minDimension - stroke
    drawRing(diameter = diameter, stroke = stroke, color = track)
    rotate(rotation - 90f) {
      drawRingArc(
        diameter = diameter,
        stroke = stroke,
        startFromTop = 90f,
        sweep = 360f * 0.32f,
        brush =
          Brush.sweepGradient(0f to colors.gradientStart, 0.32f to colors.gradientEnd, 1f to colors.gradientEnd, center = center),
        color = Color.Unspecified,
      )
    }
  }
}

private fun DrawScope.drawRing(diameter: Float, stroke: Float, color: Color) {
  drawCircle(color = color, radius = diameter / 2f, style = Stroke(width = stroke))
}

/** An arc centred in the canvas, [startFromTop] degrees clockwise from 12 o'clock. */
private fun DrawScope.drawRingArc(
  diameter: Float,
  stroke: Float,
  startFromTop: Float,
  sweep: Float,
  brush: Brush?,
  color: Color,
) {
  val topLeft = Offset(center.x - diameter / 2f, center.y - diameter / 2f)
  val arcSize = Size(diameter, diameter)
  val style = Stroke(width = stroke, cap = StrokeCap.Butt)
  if (brush != null) {
    drawArc(brush, startFromTop - 90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
  } else {
    drawArc(color, startFromTop - 90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
  }
}
