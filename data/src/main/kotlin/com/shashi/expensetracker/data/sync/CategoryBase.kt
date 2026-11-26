package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * A Categories row's mapped values as last pulled: the `base` an `UpsertCategory` or
 * `DeleteCategory` entry stores (AD-4), as JSON text. A null `base` means "absent".
 *
 * The local writes record it with [encode] from the mirror row, which holds the last-pulled values
 * while no entry is pending, so [matches] compares the name by [CategoryKey] and Color and Icon as
 * they are.
 */
@JsonClass(generateAdapter = true)
data class CategoryBase(val name: String, val color: String, val icon: String) {

  /** Whether the Sheet row [sheetRow] still holds this base's Name (by [CategoryKey]), Color and Icon. */
  fun matches(sheetRow: Category): Boolean =
    CategoryKey.of(name) == CategoryKey.of(sheetRow.name) && color == sheetRow.color && icon == sheetRow.icon

  companion object {
    private val adapter by lazy { Moshi.Builder().build().adapter(CategoryBase::class.java) }

    /** The base of [category]'s current values, as the JSON text an outbox entry stores. */
    fun encode(category: Category): String =
      adapter.toJson(CategoryBase(name = category.name, color = category.color, icon = category.icon))

    /**
     * The base stored as [json], or null for a null [json] ("absent"). Throws
     * [IllegalArgumentException] when [json] isn't a readable base.
     */
    fun decode(json: String?): CategoryBase? {
      if (json == null) return null
      return try {
        checkNotNull(adapter.fromJson(json))
      } catch (e: Exception) {
        throw IllegalArgumentException("Unreadable Category base: ${e.javaClass.simpleName}")
      }
    }
  }
}
