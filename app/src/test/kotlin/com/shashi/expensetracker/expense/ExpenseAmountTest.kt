package com.shashi.expensetracker.expense

import com.shashi.expensetracker.expense.components.formatPaise
import com.shashi.expensetracker.expense.components.parseAmountPaise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseAmountTest {

  @Test
  fun `typed rupees parse to paise`() {
    assertEquals(12_000L, parseAmountPaise("120"))
    assertEquals(12_050L, parseAmountPaise("120.5"))
    assertEquals(12_005L, parseAmountPaise(" 120.05 "))
    assertEquals(12_000L, parseAmountPaise("120."))
  }

  @Test
  fun `zero, negative, over two decimals and text are rejected`() {
    listOf("", "0", "0.00", "-5", "1.234", "abc", "1,200", ".5").forEach { assertNull(it, parseAmountPaise(it)) }
  }

  @Test
  fun `paise show as rupees`() {
    assertEquals("₹120.00", formatPaise(12_000))
    assertEquals("₹0.05", formatPaise(5))
  }
}
