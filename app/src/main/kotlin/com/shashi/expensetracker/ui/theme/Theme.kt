package com.shashi.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Mapped per DESIGN.md Colors: background/surface = surface-canvas, surfaceContainer =
// surface-container, onSurface, onSurfaceVariant, outlineVariant, primary, onPrimary = on-gradient,
// secondaryContainer = indicator, surfaceContainerHighest = ring-track, error = exceeded,
// errorContainer = exceeded-container.
//
// Derived (no DESIGN.md role): onBackground = on-surface; primaryContainer = indicator;
// onPrimaryContainer, onSecondaryContainer = on-surface; onError = on-gradient;
// surfaceContainerLowest/Low/High = surface-container; outline = on-surface-variant;
// secondary, tertiary = primary; onSecondary, onTertiary = on-gradient; tertiaryContainer =
// indicator; onTertiaryContainer = on-surface; onErrorContainer = on-surface (exceeded on
// exceeded-container is ~4.3:1, below AA); surfaceVariant =
// ring-track; surfaceTint = primary; surfaceBright = surface-container; surfaceDim = surface-canvas;
// inverseSurface = on-surface and inverseOnSurface = surface-canvas (the snackbar spec);
// inversePrimary = the other mode's primary; scrim = the scrim token.
private fun jellyPopScheme(
  base: ColorScheme,
  canvas: Color,
  container: Color,
  onSurface: Color,
  onSurfaceVariant: Color,
  outlineVariant: Color,
  primary: Color,
  inversePrimary: Color,
  onGradient: Color,
  indicator: Color,
  ringTrack: Color,
  exceeded: Color,
  exceededContainer: Color,
  scrim: Color,
): ColorScheme =
  base.copy(
    primary = primary,
    onPrimary = onGradient,
    primaryContainer = indicator,
    onPrimaryContainer = onSurface,
    inversePrimary = inversePrimary,
    secondary = primary,
    onSecondary = onGradient,
    secondaryContainer = indicator,
    onSecondaryContainer = onSurface,
    tertiary = primary,
    onTertiary = onGradient,
    tertiaryContainer = indicator,
    onTertiaryContainer = onSurface,
    background = canvas,
    onBackground = onSurface,
    surface = canvas,
    onSurface = onSurface,
    surfaceVariant = ringTrack,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = primary,
    inverseSurface = onSurface,
    inverseOnSurface = canvas,
    error = exceeded,
    onError = onGradient,
    errorContainer = exceededContainer,
    onErrorContainer = onSurface,
    outline = onSurfaceVariant,
    outlineVariant = outlineVariant,
    scrim = scrim,
    surfaceBright = container,
    surfaceDim = canvas,
    surfaceContainerLowest = container,
    surfaceContainerLow = container,
    surfaceContainer = container,
    surfaceContainerHigh = container,
    surfaceContainerHighest = ringTrack,
  )

val LightColorScheme =
  jellyPopScheme(
    base = lightColorScheme(),
    canvas = SurfaceCanvas,
    container = SurfaceContainer,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outlineVariant = OutlineVariant,
    primary = Primary,
    inversePrimary = PrimaryDark,
    onGradient = OnGradient,
    indicator = Indicator,
    ringTrack = RingTrack,
    exceeded = Exceeded,
    exceededContainer = ExceededContainer,
    scrim = Scrim,
  )

val DarkColorScheme =
  jellyPopScheme(
    base = darkColorScheme(),
    canvas = SurfaceCanvasDark,
    container = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outlineVariant = OutlineVariantDark,
    primary = PrimaryDark,
    inversePrimary = Primary,
    onGradient = OnGradientDark,
    indicator = IndicatorDark,
    ringTrack = RingTrackDark,
    exceeded = ExceededDark,
    exceededContainer = ExceededContainerDark,
    scrim = ScrimDark,
  )

/**
 * The Jelly Pop theme. Follows the system light/dark setting; dynamic color (Material You) is off
 * because the brand palette is fixed. [reduceMotion] defaults to the system "Remove animations"
 * setting.
 */
@Composable
fun ExpenseTrackerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  reduceMotion: Boolean = rememberReduceMotion(),
  content: @Composable () -> Unit,
) {
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
  CompositionLocalProvider(
    LocalExtendedColors provides extendedColors,
    LocalReduceMotion provides reduceMotion,
  ) {
    MaterialTheme(
      colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
      typography = ExpenseTrackerTypography,
      shapes = ExpenseTrackerShapes,
      content = content,
    )
  }
}

/** Accessors for theme values outside `MaterialTheme`. */
object ExpenseTrackerTheme {
  val extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

  val reduceMotion: Boolean
    @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}
