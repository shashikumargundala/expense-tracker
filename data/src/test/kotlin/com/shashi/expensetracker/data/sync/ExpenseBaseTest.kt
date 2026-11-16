package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** [ExpenseBase]'s codec and its comparison with a Sheet row (AD-4). */
class ExpenseBaseTest {

  private val expense = sampleExpense(id = "a").copy(smsId = "ref:hdfc:123:12000")

  private fun base(json: String? = ExpenseBase.encode(expense)) = checkNotNull(ExpenseBase.decode(json))

  @Test
  fun `a base round-trips every mapped value`() {
    val decoded = base()

    assertEquals(
      ExpenseBase("2026-11-13T09:30", 12_000, "Food", "Chai", "App", "ref:hdfc:123:12000"),
      decoded,
    )
    assertTrue(decoded.matches(expense))
  }

  @Test
  fun `a null base is absent`() {
    assertNull(ExpenseBase.decode(null))
  }

  @Test
  fun `an unreadable base throws IllegalArgumentException`() {
    assertThrows(IllegalArgumentException::class.java) { ExpenseBase.decode("not json") }
    assertThrows(IllegalArgumentException::class.java) { ExpenseBase.decode("{}") }
    assertThrows(IllegalArgumentException::class.java) {
      ExpenseBase.decode("""{"date":"yesterday","amountPaise":1,"category":"F","description":"","source":"App"}""")
    }
  }

  @Test
  fun `the ID is not part of the base`() {
    assertTrue(base().matches(expense.copy(id = "other")))
  }

  @Test
  fun `Category compares trimmed and case-folded`() {
    assertTrue(base().matches(expense.copy(category = " FOOD ")))
    assertTrue(base().matches(expense.copy(category = "food")))
    assertFalse(base().matches(expense.copy(category = "Fuel")))
  }

  @Test
  fun `a blank Source cell reads as User, so a base taken from one matches`() {
    val hand = expense.copy(source = ExpenseSource.User, smsId = null)

    // The read maps a blank Source to User (AD-7), which is what the mirror and the base hold.
    assertTrue(base(ExpenseBase.encode(hand)).matches(hand))
    assertFalse(base(ExpenseBase.encode(hand)).matches(hand.copy(source = ExpenseSource.App)))
  }

  @Test
  fun `a blank SMS ID is the same as none`() {
    val none = expense.copy(smsId = null)

    assertNull(base(ExpenseBase.encode(expense.copy(smsId = " "))).smsId)
    assertTrue(base(ExpenseBase.encode(none)).matches(none.copy(smsId = "")))
  }

  @Test
  fun `any other mapped value that differs fails the match`() {
    val b = base()

    assertFalse(b.matches(expense.copy(date = LocalDateTime.of(2026, 11, 13, 9, 31))))
    assertFalse(b.matches(expense.copy(amountPaise = 12_001)))
    assertFalse(b.matches(expense.copy(description = "Tea")))
    assertFalse(b.matches(expense.copy(smsId = null)))
  }
}
