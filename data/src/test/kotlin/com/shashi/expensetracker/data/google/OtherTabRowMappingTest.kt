package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AD-7 parsing of the Budgets, Dismissed and Settings tabs. */
class OtherTabRowMappingTest {

  private val budgetColumns = SheetValues.columns(listOf("Notes", "month", "Category", " Amount "))

  @Test
  fun `a Budget month reads from yyyy-MM text or a date`() {
    assertEquals(YearMonth.of(2026, 10), BudgetRowMapping.month("2026-10"))
    assertEquals(YearMonth.of(2026, 1), BudgetRowMapping.month(" 2026-1 "))
    assertEquals(YearMonth.of(2026, 11), BudgetRowMapping.month(46339.0))
    assertEquals(YearMonth.of(2026, 10), BudgetRowMapping.month("2026-10-08"))
    assertEquals(YearMonth.of(2026, 10), BudgetRowMapping.month("8 Oct 2026"))
    assertNull(BudgetRowMapping.month("October"))
    assertNull(BudgetRowMapping.month(null))
  }

  @Test
  fun `a hand-typed Budget reads by header name`() {
    assertEquals(
      ParsedRow.Valid(Budget(YearMonth.of(2026, 10), "food", 500_000)),
      BudgetRowMapping.budget(listOf("keep", "2026-10", " food ", "₹5,000"), budgetColumns),
    )
  }

  @Test
  fun `a Budget without a Month, Category or positive Amount is invalid`() {
    assertEquals(ParsedRow.Invalid, BudgetRowMapping.budget(listOf("", "", "Food", 100), budgetColumns))
    assertEquals(ParsedRow.Invalid, BudgetRowMapping.budget(listOf("", "2026-10", "", 100), budgetColumns))
    assertEquals(ParsedRow.Invalid, BudgetRowMapping.budget(listOf("", "2026-10", "Food", "abc"), budgetColumns))
    assertEquals(ParsedRow.Invalid, BudgetRowMapping.budget(listOf("", "2026-10", "Food", 0), budgetColumns))
    assertEquals(ParsedRow.Invalid, BudgetRowMapping.budget(listOf("", "soon", "Food", 100), budgetColumns))
  }

  @Test
  fun `a Budget with neither Month nor Category is unkeyed`() {
    assertEquals(ParsedRow.Unkeyed, BudgetRowMapping.budget(listOf("note", "", "", 100), budgetColumns))
  }

  @Test
  fun `a Dismissal reads its SMS ID and a serial or text date`() {
    val columns = SheetValues.columns(listOf("Dismissed On", "SMS ID"))

    assertEquals(
      ParsedRow.Valid(Dismissal("ref:hdfc:1:1000", LocalDateTime.of(2026, 11, 13, 9, 30))),
      DismissalRowMapping.dismissal(listOf(46339.395833333336, "ref:hdfc:1:1000"), columns),
    )
    assertEquals(
      ParsedRow.Valid(Dismissal("h:abc", LocalDateTime.of(2026, 10, 8, 13, 42))),
      DismissalRowMapping.dismissal(listOf("8 Oct 2026 13:42", "h:abc"), columns),
    )
    assertEquals(ParsedRow.Invalid, DismissalRowMapping.dismissal(listOf("someday", "h:abc"), columns))
    assertEquals(ParsedRow.Unkeyed, DismissalRowMapping.dismissal(listOf(46339.0, " "), columns))
  }

  @Test
  fun `a Setting reads Key and Value, a blank Value being empty`() {
    val columns = SheetValues.columns(listOf("key", "VALUE"))

    assertEquals(ParsedRow.Valid(Setting("Currency", "INR")), SettingRowMapping.setting(listOf(" Currency ", "INR"), columns))
    assertEquals(ParsedRow.Valid(Setting("Name", "")), SettingRowMapping.setting(listOf("Name"), columns))
    assertEquals(ParsedRow.Unkeyed, SettingRowMapping.setting(listOf("", "x"), columns))
  }
}
