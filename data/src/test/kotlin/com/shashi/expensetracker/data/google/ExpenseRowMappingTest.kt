package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseRowMappingTest {

  private val header = listOf<Any?>("ID", "Date", "Amount", "Category", "Description", "Source", "SMS ID")
  private val columns = ExpenseRowMapping.columns(header)

  @Test
  fun `a recorded row reads with serial date and decimal amount`() {
    val row = listOf<Any?>("0f8fad5b-d9cb-469f-a165-70867728950e", 46323.5, 250.75, "Food", "Lunch", "User", "")

    assertEquals(
      Expense(
        id = "0f8fad5b-d9cb-469f-a165-70867728950e",
        date = LocalDateTime.of(2026, 10, 28, 12, 0),
        amountPaise = 25_075,
        category = "Food",
        description = "Lunch",
        source = ExpenseSource.User,
        smsId = null,
      ),
      ExpenseRowMapping.expense(row, columns),
    )
  }

  @Test
  fun `columns are found by header name, not position, ignoring case and unknown columns`() {
    val shuffled = ExpenseRowMapping.columns(listOf("Notes", "category", "Amount", " ID ", "Date", "Source"))
    val row = listOf<Any?>("keep me", "Bills", 99, "id-1", "2026-11-13 09:30", "App")

    val expense = ExpenseRowMapping.expense(row, shuffled)!!

    assertEquals("id-1", expense.id)
    assertEquals("Bills", expense.category)
    assertEquals(9_900L, expense.amountPaise)
    assertEquals(ExpenseSource.App, expense.source)
    assertEquals("", expense.description)
  }

  @Test
  fun `hand-typed amounts accept rupee sign, separators and decimals`() {
    assertEquals(120_050L, ExpenseRowMapping.amountPaise("₹1,200.50"))
    assertEquals(12_000L, ExpenseRowMapping.amountPaise(" 120 "))
    assertEquals(12_000L, ExpenseRowMapping.amountPaise(120.0))
    assertEquals(1L, ExpenseRowMapping.amountPaise(0.005))
    assertNull(ExpenseRowMapping.amountPaise("abc"))
    assertNull(ExpenseRowMapping.amountPaise(""))
    assertNull(ExpenseRowMapping.amountPaise(0.0))
    assertNull(ExpenseRowMapping.amountPaise(-5.0))
    assertNull(ExpenseRowMapping.amountPaise(null))
  }

  @Test
  fun `paise write as two-place rupees`() {
    assertEquals("120.00", ExpenseRowMapping.formatAmount(12_000))
    assertEquals("0.05", ExpenseRowMapping.formatAmount(5))
    assertEquals("1200.50", ExpenseRowMapping.formatAmount(120_050))
  }

  @Test
  fun `dates read from serial numbers and text, to the minute`() {
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), ExpenseRowMapping.date(46339.395833333336))
    assertEquals(LocalDateTime.of(2026, 11, 13, 0, 0), ExpenseRowMapping.date(46339.0))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), ExpenseRowMapping.date("2026-11-13 09:30"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), ExpenseRowMapping.date("2026-11-13 9:30"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), ExpenseRowMapping.date("2026-11-13 09:30:59"))
    assertEquals(LocalDateTime.of(2026, 11, 13, 0, 0), ExpenseRowMapping.date("2026-11-13"))
    assertNull(ExpenseRowMapping.date("next tuesday"))
    assertNull(ExpenseRowMapping.date(""))
  }

  @Test
  fun `dates write as yyyy-MM-dd HH mm`() {
    assertEquals("2026-11-03 07:05", ExpenseRowMapping.formatDate(LocalDateTime.of(2026, 11, 3, 7, 5)))
  }

  @Test
  fun `rows missing an ID, date, amount or category are invalid`() {
    val valid = listOf<Any?>("id-1", 46339.0, 10, "Food", "", "", "")
    listOf(0, 1, 2, 3).forEach { blank ->
      val row = valid.toMutableList().also { it[blank] = "" }
      assertNull("blank column $blank", ExpenseRowMapping.expense(row, columns))
    }
    assertNull(ExpenseRowMapping.expense(listOf("id-1", 46339.0), columns))
  }

  @Test
  fun `blank or unknown Source reads as User and SMS ID blank is null`() {
    val row = listOf<Any?>("id-1", 46339.0, 10, "Food", 12.0, "  ", "ref:hdfc:1:1000")

    val expense = ExpenseRowMapping.expense(row, columns)!!

    assertEquals(ExpenseSource.User, expense.source)
    assertEquals("12", expense.description)
    assertEquals("ref:hdfc:1:1000", expense.smsId)
  }

  @Test
  fun `cells keep text as text and include Source only for a new row`() {
    val expense =
      Expense("id-1", LocalDateTime.of(2026, 11, 13, 9, 30), 12_000, "Food", "=1+1", ExpenseSource.App)

    val created = ExpenseRowMapping.cells(expense, withSource = true)
    assertEquals("'id-1", created["ID"])
    assertEquals("2026-11-13 09:30", created["Date"])
    assertEquals("120.00", created["Amount"])
    assertEquals("'Food", created["Category"])
    assertEquals("'=1+1", created["Description"])
    assertEquals("App", created["Source"])
    assertEquals("", created["SMS ID"])

    assertFalse("Source" in ExpenseRowMapping.cells(expense, withSource = false))
  }

  @Test
  fun `categories read by header name`() {
    val categoryColumns = ExpenseRowMapping.columns(listOf("Icon", "Name", "Color"))

    assertEquals(
      Category("Food", "#F59E0B", "restaurant"),
      ExpenseRowMapping.category(listOf("restaurant", " Food ", "#F59E0B"), categoryColumns),
    )
    assertEquals(Category("Fun", "", ""), ExpenseRowMapping.category(listOf(null, "Fun"), categoryColumns))
    assertNull(ExpenseRowMapping.category(listOf("x", "", "#000000"), categoryColumns))
  }

  @Test
  fun `column letters and updated ranges`() {
    assertEquals("A", ExpenseRowMapping.columnLetter(0))
    assertEquals("G", ExpenseRowMapping.columnLetter(6))
    assertEquals("Z", ExpenseRowMapping.columnLetter(25))
    assertEquals("AA", ExpenseRowMapping.columnLetter(26))
    assertEquals(12, ExpenseRowMapping.firstRow("Expenses!A12:G12"))
    assertNull(ExpenseRowMapping.firstRow(null))
  }
}
