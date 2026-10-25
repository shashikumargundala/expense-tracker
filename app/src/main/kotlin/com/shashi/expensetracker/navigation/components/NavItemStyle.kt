package com.shashi.expensetracker.navigation.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme

/** Height of every nav item, and the minimum touch target (DESIGN.md components.pill-nav). */
val NavItemHeight = 48.dp

/**
 * The darker pink DESIGN.md names for text on the gradient's pink end. White on #EC4899 fails AA,
 * so the active nav item's gradient ends here instead, and its white label passes (~4.6:1).
 */
private val ActiveNavGradientEnd = Color(0xFFDB2777)

/** Above this font scale the active nav item shows its icon only (DESIGN.md Pill nav). */
const val NavLabelMaxFontScale = 1.3f

/**
 * Whether the active nav item shows its [label]: only at font scale 1.3 or below, and only when the
 * label, measured in `labelMedium` on one line, fits in [available]. Pass the item's target width
 * (not an animating one), so the label doesn't flicker on and off while the item resizes. When this
 * is false the item shows its icon only; its TalkBack label is unchanged.
 */
@Composable
fun showActiveNavLabel(label: String, available: Dp): Boolean {
  val density = LocalDensity.current
  val style = MaterialTheme.typography.labelMedium
  val measurer = rememberTextMeasurer()
  val labelWidthPx =
    remember(label, style, density, measurer) {
      measurer.measure(label, style, maxLines = 1, softWrap = false).size.width
    }
  return density.fontScale <= NavLabelMaxFontScale && with(density) { labelWidthPx.toDp() } <= available
}

/** Fill of the active nav item: gradient-start to the darker pink, left to right. */
@Composable
@ReadOnlyComposable
fun navActiveBrush(): Brush =
  Brush.horizontalGradient(listOf(ExpenseTrackerTheme.extendedColors.gradientStart, ActiveNavGradientEnd))

/** Spring for nav size changes, or an instant change when reduce motion is on. */
@Composable
@ReadOnlyComposable
fun navSizeAnimationSpec(): AnimationSpec<Dp> =
  if (ExpenseTrackerTheme.reduceMotion) snap()
  else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
