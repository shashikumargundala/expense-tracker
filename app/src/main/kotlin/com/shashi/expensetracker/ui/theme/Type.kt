package com.shashi.expensetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// DESIGN.md typography. The M3 default font family (system sans) is kept; no custom font.
// Styles without a letterSpacing in DESIGN.md use 0, replacing the M3 default tracking.

private val SemiBold650 = FontWeight(650)
private val Base = Typography()

/** `display-amount`: hero total and form amount field. Extends M3 Headline Medium. */
val DisplayAmountTextStyle =
  Base.headlineMedium.copy(
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.02).em,
    fontFeatureSettings = "tnum",
  )

/** `amount`: row amounts. M3 Body Large at bold weight; Body Large itself stays `body`. */
val AmountTextStyle =
  Base.bodyLarge.copy(
    fontSize = 15.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.em,
    fontFeatureSettings = "tnum",
  )

val ExpenseTrackerTypography =
  Typography(
    // display-amount
    headlineMedium = DisplayAmountTextStyle,
    // screen-title
    titleLarge =
      Base.titleLarge.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
      ),
    // section-title
    titleMedium = Base.titleMedium.copy(fontSize = 16.sp, fontWeight = SemiBold650, letterSpacing = 0.em),
    // item-title
    titleSmall = Base.titleSmall.copy(fontSize = 14.sp, fontWeight = SemiBold650, letterSpacing = 0.em),
    // body
    bodyLarge =
      Base.bodyLarge.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 1.3.em,
        letterSpacing = 0.em,
      ),
    // meta
    bodySmall = Base.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.em),
    // overline: the only uppercase style. Callers uppercase the text ("OCTOBER 2026").
    labelSmall = Base.labelSmall.copy(fontSize = 11.sp, fontWeight = SemiBold650, letterSpacing = 0.05.em),
    // label
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, fontWeight = SemiBold650, letterSpacing = 0.em),
  )

/** Tabular figures, so columns of amounts line up (DESIGN.md typography.numerals). */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
