package com.shashi.expensetracker.expense.components

import java.math.BigDecimal

/**
 * Minimal rupee text for the tracer list, for example `₹120.00`. Placeholder until the designed money
 * formatter (with the Currency setting) replaces it; with the form's [parseAmountPaise] it is the
 * `:app` side of AD-6.
 */
internal fun formatPaise(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).toPlainString()

/**
 * Typed amount text as paise: digits with at most two decimals, for example `120` or `120.5`. Null
 * for anything else, including zero.
 */
internal fun parseAmountPaise(text: String): Long? {
  val value = text.trim()
  if (!Regex("""\d{1,9}(\.\d{0,2})?""").matches(value)) return null
  val paise = BigDecimal(value.trimEnd('.')).movePointRight(2).longValueExact()
  return paise.takeIf { it > 0 }
}
