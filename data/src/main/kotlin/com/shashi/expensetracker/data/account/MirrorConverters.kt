package com.shashi.expensetracker.data.account

import androidx.room.TypeConverter
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Room column types for the mirror. Dates are ISO text with no zone (`2026-11-13T09:30`, AD-19),
 * months are `yyyy-MM`, and enums are stored by name, so renaming a constant needs a migration.
 */
class MirrorConverters {
  @TypeConverter fun dateToText(value: LocalDateTime?): String? = value?.toString()

  @TypeConverter fun textToDate(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

  @TypeConverter fun monthToText(value: YearMonth?): String? = value?.toString()

  @TypeConverter fun textToMonth(value: String?): YearMonth? = value?.let(YearMonth::parse)

  @TypeConverter fun sourceToText(value: ExpenseSource?): String? = value?.name

  @TypeConverter fun textToSource(value: String?): ExpenseSource? = value?.let(ExpenseSource::valueOf)

  @TypeConverter fun outboxTypeToText(value: OutboxType?): String? = value?.name

  @TypeConverter fun textToOutboxType(value: String?): OutboxType? = value?.let(OutboxType::valueOf)
}
