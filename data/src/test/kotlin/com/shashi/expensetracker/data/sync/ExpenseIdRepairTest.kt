package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AD-5's ID repair rules on one read, with no network. */
class ExpenseIdRepairTest {

  private var next = 0

  private fun plan(vararg rows: SheetRow<ExpenseRowRead>) = ExpenseIdRepair.plan(rows.toList()) { "new-${++next}" }

  private fun row(row: Int, expense: Expense, sourceBlank: Boolean = false) =
    SheetRow(row, ExpenseRowRead(expense.id, expense.copy(source = if (sourceBlank) ExpenseSource.User else expense.source), sourceBlank))

  private fun blankId(row: Int, expense: Expense, sourceBlank: Boolean = true) =
    SheetRow(row, ExpenseRowRead(null, expense.copy(id = "", source = ExpenseSource.User), sourceBlank))

  private fun invalid(row: Int, id: String?) = SheetRow(row, ExpenseRowRead(id, null, sourceBlank = true))

  @Test
  fun `a clean read needs nothing`() {
    val result = plan(row(2, sampleExpense(id = "a")), row(3, sampleExpense(id = "b")))

    assertTrue(result.isEmpty)
    assertFalse(result.changesIds)
  }

  @Test
  fun `a blank ID gets a new UUID and a blank Source gets User`() {
    val result = plan(blankId(2, sampleExpense()), blankId(3, sampleExpense(), sourceBlank = false))

    assertEquals(
      listOf(ExpenseRowFix(2, id = "new-1", sourceUser = true), ExpenseRowFix(3, id = "new-2")),
      result.fixes,
    )
    assertTrue(result.deletes.isEmpty())
    assertTrue(result.changesIds)
  }

  @Test
  fun `a keyed row with a blank Source gets only User written`() {
    val result = plan(row(2, sampleExpense(id = "a"), sourceBlank = true))

    assertEquals(listOf(ExpenseRowFix(2, sourceUser = true)), result.fixes)
    assertFalse(result.changesIds)
  }

  @Test
  fun `the real default is a lowercase UUID`() {
    val id = checkNotNull(ExpenseIdRepair.plan(listOf(blankId(2, sampleExpense()))).fixes.single().id)

    assertEquals(id.lowercase(), id)
    assertEquals(36, id.length)
  }

  @Test
  fun `an identical later row is a retry duplicate and is deleted`() {
    val x = sampleExpense(id = "x").copy(smsId = "s")

    val result = plan(row(5, x), row(9, x.copy(category = "FOOD")))

    assertTrue(result.fixes.isEmpty())
    assertEquals(listOf(9), result.deletes)
  }

  @Test
  fun `a blank Source equals User when comparing, and the deleted copy gets no fix`() {
    val x = sampleExpense(id = "x").copy(source = ExpenseSource.User)

    val result = plan(row(5, x), row(9, x, sourceBlank = true))

    assertEquals(listOf(9), result.deletes)
    assertTrue(result.fixes.isEmpty())
  }

  @Test
  fun `a differing later row gets a new UUID and its SMS ID cleared`() {
    val x = sampleExpense(id = "x")

    val result = plan(row(5, x), row(9, x.copy(amountPaise = 999, smsId = "s")))

    assertEquals(listOf(ExpenseRowFix(9, id = "new-1", clearSmsId = true)), result.fixes)
    assertTrue(result.deletes.isEmpty())
  }

  @Test
  fun `each mapped value counts as a difference`() {
    val x = sampleExpense(id = "x").copy(smsId = "s")
    val variants =
      listOf(
        x.copy(date = x.date.plusMinutes(1)),
        x.copy(amountPaise = 1),
        x.copy(category = "Bills"),
        x.copy(description = "Tea"),
        x.copy(source = ExpenseSource.User),
        x.copy(smsId = null),
      )

    variants.forEach { variant ->
      val result = plan(row(2, x), row(3, variant))
      assertEquals(variant.toString(), listOf(3), result.fixes.map { it.row })
      assertTrue(result.deletes.isEmpty())
    }
  }

  @Test
  fun `the first in sheet order keeps the ID in a mixed group`() {
    val x = sampleExpense(id = "x")
    val other = x.copy(amountPaise = 1)

    val result = plan(row(4, x), row(6, other, sourceBlank = true), row(7, x), row(12, x), row(13, other, sourceBlank = true))

    assertEquals(listOf(ExpenseRowFix(6, id = "new-1", sourceUser = true)), result.fixes)
    assertEquals(listOf(7, 12, 13), result.deletes)
  }

  @Test
  fun `two identical later copies that differ from the first become one new Expense`() {
    val a = sampleExpense(id = "x")
    val b = a.copy(amountPaise = 1, smsId = "s")

    val result = plan(row(4, a), row(6, b), row(13, b))

    assertEquals(listOf(ExpenseRowFix(6, id = "new-1", clearSmsId = true)), result.fixes)
    assertEquals(listOf(13), result.deletes)
  }

  @Test
  fun `invalid rows are never fixed or deleted, but an invalid first row keeps its ID`() {
    val x = sampleExpense(id = "x").copy(smsId = "s")

    val result = plan(invalid(2, "x"), invalid(3, null), row(4, x), row(5, x), invalid(6, "x"))

    assertEquals(listOf(ExpenseRowFix(4, id = "new-1", clearSmsId = true)), result.fixes)
    assertEquals(listOf(5), result.deletes)
  }

  @Test
  fun `an invalid row after a valid first row changes nothing`() {
    val x = sampleExpense(id = "x")

    assertTrue(plan(row(2, x), invalid(3, "x")).isEmpty)
  }
}
