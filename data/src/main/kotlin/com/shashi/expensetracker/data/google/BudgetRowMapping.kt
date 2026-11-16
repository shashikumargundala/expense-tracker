package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.google.ExpenseSheetLayout.AMOUNT
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.CATEGORY
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.MONTH
import com.shashi.expensetracker.data.google.SheetValues.cell
import com.shashi.expensetracker.data.google.SheetValues.text
import com.shashi.expensetracker.domain.budget.Budget
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Turns Budgets rows into [Budget]s (AD-7, AD-8). */
internal object BudgetRowMapping {

  val requiredHeaders = listOf(MONTH, CATEGORY, AMOUNT)

  private val monthText = DateTimeFormatter.ofPattern("yyyy-M")

  /**
   * The Budget in [row]. With neither Month nor Category it is [ParsedRow.Unkeyed]; a missing or
   * unreadable Month, a blank Category or an Amount that isn't above zero is [ParsedRow.Invalid].
   */
  fun budget(row: List<Any?>, columns: Map<String, Int>): ParsedRow<Budget> {
    val monthCell = cell(row, columns, MONTH)
    val category = text(cell(row, columns, CATEGORY))
    if (text(monthCell) == null && category == null) return ParsedRow.Unkeyed
    val month = month(monthCell) ?: return ParsedRow.Invalid
    val amount = SheetValues.amountPaise(cell(row, columns, AMOUNT)) ?: return ParsedRow.Invalid
    return ParsedRow.Valid(Budget(month = month, category = category ?: return ParsedRow.Invalid, amountPaise = amount))
  }

  /** A Month cell: `yyyy-MM` text, or any date [SheetValues.date] reads (its month). Null otherwise. */
  fun month(cell: Any?): YearMonth? {
    if (cell is String) {
      try {
        return YearMonth.parse(cell.trim(), monthText)
      } catch (e: DateTimeParseException) {
        // Fall back to a date.
      }
    }
    return SheetValues.date(cell)?.let(YearMonth::from)
  }
}
