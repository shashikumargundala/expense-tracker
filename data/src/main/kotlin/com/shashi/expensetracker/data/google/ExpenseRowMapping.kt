package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Turns Expenses and Categories rows into domain values and back (AD-7). Columns are found by
 * header name, never by position. This and the `:app` formatter are the only places that convert
 * paise to decimal rupees (AD-6).
 *
 * Reads come with `UNFORMATTED_VALUE` and `SERIAL_NUMBER` dates, so a cell is a number, a string, a
 * boolean or null; hand-typed text dates and amounts (`₹1,200.50`) are accepted too.
 */
internal object ExpenseRowMapping {

  /** The Expenses headers a row needs to be read; Description, Source and SMS ID may be absent. */
  val requiredExpenseHeaders =
    listOf(ExpenseSheetLayout.ID, ExpenseSheetLayout.DATE, ExpenseSheetLayout.AMOUNT, ExpenseSheetLayout.CATEGORY)

  /** `Date` as written to the Sheet and shown there (AD-7, Consistency Conventions). */
  private val sheetDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  private val textDates =
    listOf(
      sheetDate,
      DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )

  /** Day 0 of Sheets' serial dates. */
  private val serialEpoch = LocalDateTime.of(1899, 12, 30, 0, 0)
  private const val MINUTES_PER_DAY = 24 * 60

  /**
   * Header name (trimmed) to its 0-based column. Matching ignores case; the first of two equal
   * headers wins.
   */
  fun columns(header: List<Any?>): Map<String, Int> {
    val byName = linkedMapOf<String, Int>()
    header.forEachIndexed { index, cell ->
      val name = text(cell)?.lowercase() ?: return@forEachIndexed
      byName.putIfAbsent(name, index)
    }
    return byName
  }

  /** Whether [columns] has every header in [names]. */
  fun hasAll(columns: Map<String, Int>, names: List<String>): Boolean =
    names.all { it.lowercase() in columns }

  /** The column of [name] in [columns], or null if the header is absent. */
  fun column(columns: Map<String, Int>, name: String): Int? = columns[name.lowercase()]

  /** Whether every cell of [row] is blank, as for an empty row between rows. */
  fun isBlank(row: List<Any?>): Boolean = row.all { text(it) == null }

  /** The Expense in [row], or null if its ID, Date, Amount or Category can't be read. */
  fun expense(row: List<Any?>, columns: Map<String, Int>): Expense? {
    fun cell(name: String): Any? = column(columns, name)?.let(row::getOrNull)
    val id = text(cell(ExpenseSheetLayout.ID)) ?: return null
    val date = date(cell(ExpenseSheetLayout.DATE)) ?: return null
    val amount = amountPaise(cell(ExpenseSheetLayout.AMOUNT)) ?: return null
    val category = text(cell(ExpenseSheetLayout.CATEGORY)) ?: return null
    return Expense(
      id = id,
      date = date,
      amountPaise = amount,
      category = category,
      description = text(cell(ExpenseSheetLayout.DESCRIPTION)).orEmpty(),
      // A blank or unknown Source reads as User (AD-7).
      source = if (text(cell(ExpenseSheetLayout.SOURCE)).equals("App", ignoreCase = true)) ExpenseSource.App else ExpenseSource.User,
      smsId = text(cell(ExpenseSheetLayout.SMS_ID)),
    )
  }

  /** The Category in [row], or null without a Name. */
  fun category(row: List<Any?>, columns: Map<String, Int>): Category? {
    fun cell(name: String): Any? = column(columns, name)?.let(row::getOrNull)
    val name = text(cell(ExpenseSheetLayout.NAME)) ?: return null
    return Category(
      name = name,
      color = text(cell(ExpenseSheetLayout.COLOR)).orEmpty(),
      icon = text(cell(ExpenseSheetLayout.ICON)).orEmpty(),
    )
  }

  /**
   * The cells to write for [expense], header name to `USER_ENTERED` value. Source is included only
   * for a new row ([withSource]): app edits never change an existing Source (AD-7). Text is written
   * with a leading `'` so the Sheet keeps it as typed (a Description of `12` or `=1+1` stays text);
   * the apostrophe is not part of the cell's value.
   */
  fun cells(expense: Expense, withSource: Boolean): Map<String, String> =
    buildMap {
      put(ExpenseSheetLayout.ID, asText(expense.id))
      put(ExpenseSheetLayout.DATE, formatDate(expense.date))
      put(ExpenseSheetLayout.AMOUNT, formatAmount(expense.amountPaise))
      put(ExpenseSheetLayout.CATEGORY, asText(expense.category))
      put(ExpenseSheetLayout.DESCRIPTION, asText(expense.description))
      if (withSource) put(ExpenseSheetLayout.SOURCE, expense.source.name)
      put(ExpenseSheetLayout.SMS_ID, asText(expense.smsId.orEmpty()))
    }

  /** Paise as decimal rupees with two places, for example `12000` as `120.00`. */
  fun formatAmount(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()

  /** [date] as `yyyy-MM-dd HH:mm`, which `USER_ENTERED` stores as a date. */
  fun formatDate(date: LocalDateTime): String = date.format(sheetDate)

  /**
   * An amount cell as paise: a number, or text with an optional `₹`, thousands separators and
   * decimals. Fractions of a paisa round half up. Null for a blank, unreadable or non-positive amount.
   */
  fun amountPaise(cell: Any?): Long? {
    val decimal =
      when (cell) {
        is Number -> cell.toString().toBigDecimalOrNull()
        is String -> cell.replace("₹", "").replace(",", "").replace(" ", "").trim().toBigDecimalOrNull()
        else -> null
      } ?: return null
    val paise =
      try {
        decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
      } catch (e: ArithmeticException) {
        return null
      }
    return paise.takeIf { it > 0 }
  }

  /**
   * A Date cell as a minute-precision [LocalDateTime]: a serial number (days since 1899-12-30, the
   * fraction is the time of day) or text such as `2026-11-13 09:30` or `2026-11-13`. Null if blank or
   * unreadable.
   */
  fun date(cell: Any?): LocalDateTime? =
    when (cell) {
      is Number -> {
        val minutes = (cell.toDouble() * MINUTES_PER_DAY).let { if (it.isFinite()) Math.round(it) else null }
        minutes?.let { serialEpoch.plusMinutes(it) }
      }
      is String -> textDate(cell.trim())
      else -> null
    }

  private fun textDate(value: String): LocalDateTime? {
    if (value.isEmpty()) return null
    textDates.forEach { format ->
      try {
        return LocalDateTime.parse(value, format).truncatedTo(ChronoUnit.MINUTES)
      } catch (e: DateTimeParseException) {
        // Try the next format.
      }
    }
    return try {
      LocalDate.parse(value).atStartOfDay()
    } catch (e: DateTimeParseException) {
      null
    }
  }

  /**
   * A cell as trimmed text, or null when blank. A number reads without a trailing `.0`, so a
   * hand-typed `12` Description stays `12`.
   */
  fun text(cell: Any?): String? {
    val value =
      when (cell) {
        null -> return null
        is Double -> BigDecimal(cell.toString()).stripTrailingZeros().toPlainString()
        is Float -> BigDecimal(cell.toString()).stripTrailingZeros().toPlainString()
        else -> cell.toString()
      }.trim()
    return value.ifEmpty { null }
  }

  /** The A1 letters of the 0-based [index]: 0 is `A`, 25 is `Z`, 26 is `AA`. */
  fun columnLetter(index: Int): String {
    var n = index + 1
    val letters = StringBuilder()
    while (n > 0) {
      val rem = (n - 1) % 26
      letters.append('A' + rem)
      n = (n - 1) / 26
    }
    return letters.reverse().toString()
  }

  /** The first row number in an A1 range such as `Expenses!A12:G12`, or null. */
  fun firstRow(range: String?): Int? =
    range?.let { Regex("![A-Za-z]*(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }

  private fun asText(value: String): String = if (value.isEmpty()) "" else "'$value"
}
