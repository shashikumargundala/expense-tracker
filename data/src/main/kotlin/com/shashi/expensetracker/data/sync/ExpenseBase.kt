package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.category.CategoryKey
import com.shashi.expensetracker.domain.expense.Expense
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.time.LocalDateTime

/**
 * An Expense row's mapped values as last pulled: the `base` an outbox entry stores (AD-4), as JSON
 * text. A null `base` means "absent". The ID isn't part of it, because the entry's `row_key` is.
 *
 * [date] is ISO `yyyy-MM-ddTHH:mm` text, [source] is an `ExpenseSource` name, and a blank [smsId]
 * is stored as null.
 *
 * Edits and deletes (entries 7 and 10) record a base with [encode] from the mirror row, which holds
 * the last-pulled values with the Category spelled as its Category row (AD-9). So [matches] compares
 * Category by [CategoryKey].
 */
@JsonClass(generateAdapter = true)
data class ExpenseBase(
  val date: String,
  val amountPaise: Long,
  val category: String,
  val description: String,
  val source: String,
  val smsId: String? = null,
) {

  /**
   * Whether the Sheet row [sheetRow] still holds this base's values: Date, Amount, Category (by
   * [CategoryKey]), Description, Source and SMS ID are all equal. A row read from a blank Source
   * cell already reads as `User` (AD-7). Unmapped columns are never compared.
   */
  fun matches(sheetRow: Expense): Boolean =
    LocalDateTime.parse(date) == sheetRow.date &&
      amountPaise == sheetRow.amountPaise &&
      CategoryKey.of(category) == CategoryKey.of(sheetRow.category) &&
      description == sheetRow.description &&
      source == sheetRow.source.name &&
      smsId.orNullIfBlank() == sheetRow.smsId.orNullIfBlank()

  companion object {
    private val adapter by lazy { Moshi.Builder().build().adapter(ExpenseBase::class.java) }

    /** The base of [expense]'s current values, as the JSON text an outbox entry stores. */
    fun encode(expense: Expense): String =
      adapter.toJson(
        ExpenseBase(
          date = expense.date.toString(),
          amountPaise = expense.amountPaise,
          category = expense.category,
          description = expense.description,
          source = expense.source.name,
          smsId = expense.smsId.orNullIfBlank(),
        )
      )

    /**
     * The base stored as [json], or null for a null [json] ("absent"). Throws
     * [IllegalArgumentException] when [json] isn't a readable base.
     */
    fun decode(json: String?): ExpenseBase? {
      if (json == null) return null
      return try {
        checkNotNull(adapter.fromJson(json)).also { LocalDateTime.parse(it.date) }
      } catch (e: Exception) {
        throw IllegalArgumentException("Unreadable Expense base: ${e.javaClass.simpleName}")
      }
    }

    private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
  }
}
