package com.shashi.expensetracker.data.google

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The value and column helpers every tab's mapping shares (AD-7). */
class SheetValuesTest {

  @Test
  fun `hand-typed amounts accept rupee sign, separators and decimals`() {
    assertEquals(120_050L, SheetValues.amountPaise("₹1,200.50"))
    assertEquals(12_000L, SheetValues.amountPaise(" 120 "))
    assertEquals(12_000L, SheetValues.amountPaise(120.0))
    assertEquals(1L, SheetValues.amountPaise(0.005))
    assertNull(SheetValues.amountPaise("abc"))
    assertNull(SheetValues.amountPaise(""))
    assertNull(SheetValues.amountPaise(0.0))
    assertNull(SheetValues.amountPaise(-5.0))
    assertNull(SheetValues.amountPaise(null))
  }

  @Test
  fun `paise write as two-place rupees`() {
    assertEquals("120.00", SheetValues.formatAmount(12_000))
    assertEquals("0.05", SheetValues.formatAmount(5))
    assertEquals("1200.50", SheetValues.formatAmount(120_050))
  }

  @Test
  fun `dates read from serial numbers and text, to the minute`() {
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), SheetValues.date(46339.395833333336))
    assertEquals(LocalDateTime.of(2026, 11, 13, 0, 0), SheetValues.date(46339.0))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), SheetValues.date("2026-11-13 09:30"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), SheetValues.date("2026-11-13 9:30"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), SheetValues.date("2026-11-13 09:30:59"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 0, 0), SheetValues.date("2026-11-13"))
    assertEquals(LocalDateTime.of(2026, 10, 8, 13, 42), SheetValues.date("8 Oct 2026 13:42"))
    assertEquals(LocalDateTime.of(2026, 10, 8, 9, 5), SheetValues.date("08 oct 2026 9:05"))
    assertEquals(LocalDateTime.of(2026, 10, 8, 0, 0), SheetValues.date("8 Oct 2026"))
    assertNull(SheetValues.date("next tuesday"))
    assertNull(SheetValues.date(""))
    assertNull(SheetValues.date(null))
  }

  @Test
  fun `dates write as yyyy-MM-dd HH mm`() {
    assertEquals("2026-11-03 07:05", SheetValues.formatDate(LocalDateTime.of(2026, 11, 3, 7, 5)))
  }

  @Test
  fun `headers match trimmed and case-insensitive, the first of two winning`() {
    val columns = SheetValues.columns(listOf(" sms id ", null, "Amount", "AMOUNT"))

    assertEquals(0, SheetValues.column(columns, "SMS ID"))
    assertEquals(2, SheetValues.column(columns, "amount"))
    assertNull(SheetValues.column(columns, "Date"))
    assertEquals(true, SheetValues.hasAll(columns, listOf("SMS ID", "Amount")))
    assertEquals(false, SheetValues.hasAll(columns, listOf("SMS ID", "Date")))
  }

  @Test
  fun `column letters and updated ranges`() {
    assertEquals("A", SheetValues.columnLetter(0))
    assertEquals("G", SheetValues.columnLetter(6))
    assertEquals("Z", SheetValues.columnLetter(25))
    assertEquals("AA", SheetValues.columnLetter(26))
    assertEquals(12, SheetValues.firstRow("Expenses!A12:G12"))
    assertNull(SheetValues.firstRow(null))
  }
}
