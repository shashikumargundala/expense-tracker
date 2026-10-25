package com.shashi.expensetracker.ui.theme

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Compares every DESIGN.md color token against what ExpenseTrackerTheme actually provides.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ThemeTokensTest {

  @get:Rule val composeRule = createComposeRule()

  /** Where each DESIGN.md token lives in the theme. A token missing here fails the test. */
  private val resolvers: Map<String, (ColorScheme, ExtendedColors) -> Color> =
    mapOf(
      "surface-canvas" to { s, _ -> s.background },
      "surface-container" to { s, _ -> s.surfaceContainer },
      "on-surface" to { s, _ -> s.onSurface },
      "on-surface-variant" to { s, _ -> s.onSurfaceVariant },
      "outline-variant" to { s, _ -> s.outlineVariant },
      "edge" to { _, e -> e.edge },
      "primary" to { s, _ -> s.primary },
      "gradient-start" to { _, e -> e.gradientStart },
      "gradient-end" to { _, e -> e.gradientEnd },
      "on-gradient" to { _, e -> e.onGradient },
      "on-primary" to { s, _ -> s.onPrimary },
      "on-exceeded" to { s, _ -> s.onError },
      "indicator" to { s, _ -> s.secondaryContainer },
      "ring-track" to { s, _ -> s.surfaceContainerHighest },
      "warning" to { _, e -> e.warning },
      "warning-container" to { _, e -> e.warningContainer },
      "exceeded" to { s, _ -> s.error },
      "exceeded-container" to { s, _ -> s.errorContainer },
      "pending-sync" to { _, e -> e.pendingSync },
      "pending-sync-container" to { _, e -> e.pendingSyncContainer },
      "inbox" to { _, e -> e.inbox },
      "inbox-container" to { _, e -> e.inboxContainer },
      "category-food" to { _, e -> e.categoryFood },
      "category-food-tint" to { _, e -> e.categoryFoodTint },
      "category-transport" to { _, e -> e.categoryTransport },
      "category-transport-tint" to { _, e -> e.categoryTransportTint },
      "category-shopping" to { _, e -> e.categoryShopping },
      "category-shopping-tint" to { _, e -> e.categoryShoppingTint },
      "category-bills" to { _, e -> e.categoryBills },
      "category-bills-tint" to { _, e -> e.categoryBillsTint },
      "category-fun" to { _, e -> e.categoryFun },
      "category-fun-tint" to { _, e -> e.categoryFunTint },
      "aurora-1" to { _, e -> e.aurora1 },
      "aurora-2" to { _, e -> e.aurora2 },
      "aurora-3" to { _, e -> e.aurora3 },
      "glass" to { _, e -> e.glass },
      "glass-edge" to { _, e -> e.glassEdge },
      "nav-glass" to { _, e -> e.navGlass },
      "scrim" to { _, e -> e.scrim },
    )

  @Test fun lightThemeMatchesEveryLightToken() = assertTokens(dark = false)

  @Test fun darkThemeMatchesEveryDarkToken() = assertTokens(dark = true)

  @Test
  @Config(qualifiers = "notnight")
  fun followsSystemLightMode() {
    assertEquals(SurfaceCanvas, renderSystemDefault().background)
  }

  @Test
  @Config(qualifiers = "night")
  fun followsSystemDarkMode() {
    assertEquals(SurfaceCanvasDark, renderSystemDefault().background)
  }

  @Test
  fun darkOnPrimaryAndBadgeTextMeetAA() {
    val (scheme, _) = render(dark = true)
    val onPrimary = contrast(scheme.onPrimary, scheme.primary)
    val badge = contrast(scheme.onError, scheme.error)
    assertTrue("dark onPrimary on primary is $onPrimary:1", onPrimary >= 4.5)
    assertTrue("dark badge text (onError) on error is $badge:1", badge >= 4.5)
  }

  /** WCAG 2 contrast ratio of two opaque colors. */
  private fun contrast(a: Color, b: Color): Double {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05) / (lo + 0.05)
  }

  private fun assertTokens(dark: Boolean) {
    val all = DesignTokens.colorTokens()
    val tokens =
      if (dark) all.filterKeys { it.endsWith("-dark") }.mapKeys { it.key.removeSuffix("-dark") }
      else all.filterKeys { !it.endsWith("-dark") }
    // DESIGN.md front matter holds 39 color tokens per mode: 22 base (on-primary and on-exceeded
    // added in story 1.8), 10 category and 7 derived. Every light token must have a -dark twin.
    assertEquals("DESIGN.md color token count (${if (dark) "dark" else "light"})", 39, tokens.size)
    assertEquals(
      "Light and dark token names differ",
      all.keys.filterNot { it.endsWith("-dark") }.toSet(),
      all.keys.filter { it.endsWith("-dark") }.map { it.removeSuffix("-dark") }.toSet(),
    )

    val (scheme, extended) = render(dark)
    val missing = tokens.keys - resolvers.keys
    assertTrue("Tokens the theme doesn't expose: $missing", missing.isEmpty())

    val mismatches =
      tokens.mapNotNull { (name, hex) ->
        val expected = DesignTokens.parseDesignHex(hex)
        val actual = resolvers.getValue(name)(scheme, extended)
        if (expected == actual) null else "$name: DESIGN.md $hex, theme $actual"
      }
    assertTrue("Mismatched tokens:\n${mismatches.joinToString("\n")}", mismatches.isEmpty())
    // DESIGN.md maps surface-canvas to both background and surface.
    assertEquals(DesignTokens.parseDesignHex(tokens.getValue("surface-canvas")), scheme.surface)
  }

  private fun render(dark: Boolean): Pair<ColorScheme, ExtendedColors> {
    var scheme: ColorScheme? = null
    var extended: ExtendedColors? = null
    composeRule.setContent {
      ExpenseTrackerTheme(darkTheme = dark, reduceMotion = false) {
        scheme = MaterialTheme.colorScheme
        extended = LocalExtendedColors.current
      }
    }
    composeRule.waitForIdle()
    return scheme!! to extended!!
  }

  private fun renderSystemDefault(): ColorScheme {
    var scheme: ColorScheme? = null
    composeRule.setContent { ExpenseTrackerTheme { scheme = MaterialTheme.colorScheme } }
    composeRule.waitForIdle()
    return scheme!!
  }
}
