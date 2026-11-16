package com.shashi.expensetracker.data.google

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

/** How one Sheet row read: a value, an invalid row (counted as skipped), or a row with a blank key. */
internal sealed interface ParsedRow<out T> {
  data class Valid<T>(val value: T) : ParsedRow<T>

  /** The row has its key but a missing or unreadable mapped value; it is skipped and counted (AD-7). */
  data object Invalid : ParsedRow<Nothing>

  /** The row's key is blank; it is left out and not counted (entry 3 repairs blank IDs). */
  data object Unkeyed : ParsedRow<Nothing>
}

/**
 * The column and value helpers every tab's mapping shares (AD-7). Columns are found by header name,
 * trimmed and case-insensitive, never by position. This and the `:app` formatter are the only places
 * that convert paise to decimal rupees (AD-6).
 *
 * Reads come with `UNFORMATTED_VALUE` and `SERIAL_NUMBER` dates, so a cell is a number, a string, a
 * boolean or null; hand-typed text dates (`8 Oct 2026 13:42`) and amounts (`₹1,200.50`) are
 * accepted too.
 */
internal object SheetValues {

  /** `Date` as written to the Sheet and shown there (AD-7, Consistency Conventions). */
  private val sheetDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  private val textDateTimes =
    listOf(
      sheetDate,
      DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ISO_LOCAL_DATE_TIME,
      textFormat("d MMM yyyy H:mm"),
      textFormat("d MMM yyyy H:mm:ss"),
    )
  private val textDates = listOf(DateTimeFormatter.ISO_LOCAL_DATE, textFormat("d MMM yyyy"))

  /** Day 0 of Sheets' serial dates. */
  private val serialEpoch = LocalDateTime.of(1899, 12, 30, 0, 0)
  private const val MINUTES_PER_DAY = 24 * 60

  private fun textFormat(pattern: String): DateTimeFormatter =
    DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH)

  /**
   * Header name (trimmed, lowercase) to its 0-based column. The first of two equal headers wins.
   */
  fun columns(header: List<Any?>): Map<String, Int> {
    val byName = linkedMapOf<String, Int>()
    header.forEachIndexed { index, cell ->
      val name = text(cell)?.lowercase(Locale.ROOT) ?: return@forEachIndexed
      byName.putIfAbsent(name, index)
    }
    return byName
  }

  /** Whether [columns] has every header in [names]. */
  fun hasAll(columns: Map<String, Int>, names: List<String>): Boolean = names.all { column(columns, it) != null }

  /** The column of [name] in [columns], or null if the header is absent. */
  fun column(columns: Map<String, Int>, name: String): Int? = columns[name.trim().lowercase(Locale.ROOT)]

  /** The cell of [row] under header [name], or null if the header or the cell is absent. */
  fun cell(row: List<Any?>, columns: Map<String, Int>, name: String): Any? = column(columns, name)?.let(row::getOrNull)

  /** Whether every cell of [row] is blank, as for an empty row between rows. */
  fun isBlank(row: List<Any?>): Boolean = row.all { text(it) == null }

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

  /** Paise as decimal rupees with two places, for example `12000` as `120.00`. */
  fun formatAmount(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()

  /** [date] as `yyyy-MM-dd HH:mm`, which `USER_ENTERED` stores as a date. */
  fun formatDate(date: LocalDateTime): String = date.format(sheetDate)

  /**
   * A date cell as a minute-precision [LocalDateTime]: a serial number (days since 1899-12-30, the
   * fraction is the time of day) or text such as `2026-11-13 09:30`, `2026-11-13`, `8 Oct 2026 13:42`
   * or `8 Oct 2026`. Null if blank or unreadable.
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
    textDateTimes.forEach { format ->
      try {
        return LocalDateTime.parse(value, format).truncatedTo(ChronoUnit.MINUTES)
      } catch (e: DateTimeParseException) {
        // Try the next format.
      }
    }
    textDates.forEach { format ->
      try {
        return LocalDate.parse(value, format).atStartOfDay()
      } catch (e: DateTimeParseException) {
        // Try the next format.
      }
    }
    return null
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

  /**
   * [value] for a `USER_ENTERED` write that keeps it as typed: a leading `'` makes the Sheet store
   * text (a Description of `12` or `=1+1` stays text); the apostrophe is not part of the cell's value.
   */
  fun asText(value: String): String = if (value.isEmpty()) "" else "'$value"

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
}
