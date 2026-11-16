package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExpenseRowMappingTest {

  private val header = listOf<Any?>("ID", "Date", "Amount", "Category", "Description", "Source", "SMS ID")
  private val columns = SheetValues.columns(header)

  private fun read(row: List<Any?>, columns: Map<String, Int> = this.columns): Expense =
    (ExpenseRowMapping.expense(row, columns) as ParsedRow.Valid).value

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
      read(row),
    )
  }

  @Test
  fun `a hand-typed row reads its rupee amount, text date, trimmed Category and blank Source`() {
    val row = listOf<Any?>("hand-1", "8 Oct 2026 13:42", "₹1,200.50", " food ", "Dinner", "", "")

    val expense = read(row)

    assertEquals(120_050L, expense.amountPaise)
    assertEquals(LocalDateTime.of(2026, 10, 8, 13, 42), expense.date)
    assertEquals("food", expense.category)
    assertEquals(ExpenseSource.User, expense.source)
  }

  @Test
  fun `columns are found by header name, not position, ignoring case and unknown columns`() {
    val shuffled = SheetValues.columns(listOf("Notes", "category", "Amount", " ID ", "Date", "Source"))
    val row = listOf<Any?>("keep me", "Bills", 99, "id-1", "2026-11-13 09:30", "App")

    val expense = read(row, shuffled)

    assertEquals("id-1", expense.id)
    assertEquals("Bills", expense.category)
    assertEquals(9_900L, expense.amountPaise)
    assertEquals(ExpenseSource.App, expense.source)
    assertEquals("", expense.description)
  }

  @Test
  fun `a row with an ID but a missing or unreadable Date, Amount or Category is invalid`() {
    val valid = listOf<Any?>("id-1", 46339.0, 10, "Food", "", "", "")
    listOf(1, 2, 3).forEach { blank ->
      val row = valid.toMutableList().also { it[blank] = "" }
      assertEquals("blank column $blank", ParsedRow.Invalid, ExpenseRowMapping.expense(row, columns))
    }
    assertEquals(ParsedRow.Invalid, ExpenseRowMapping.expense(valid.toMutableList().also { it[2] = "abc" }, columns))
    assertEquals(ParsedRow.Invalid, ExpenseRowMapping.expense(valid.toMutableList().also { it[1] = "someday" }, columns))
    assertEquals(ParsedRow.Invalid, ExpenseRowMapping.expense(listOf("id-1", 46339.0), columns))
  }

  @Test
  fun `a row with a blank ID is unkeyed, not invalid`() {
    assertEquals(ParsedRow.Unkeyed, ExpenseRowMapping.expense(listOf("  ", 46339.0, 10, "Food"), columns))
  }

  @Test
  fun `blank or unknown Source reads as User and SMS ID blank is null`() {
    val row = listOf<Any?>("id-1", 46339.0, 10, "Food", 12.0, "  ", "ref:hdfc:1:1000")

    val expense = read(row)

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
}
