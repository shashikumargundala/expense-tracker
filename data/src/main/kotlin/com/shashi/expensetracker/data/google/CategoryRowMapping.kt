package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.google.ExpenseSheetLayout.COLOR
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.ICON
import com.shashi.expensetracker.data.google.ExpenseSheetLayout.NAME
import com.shashi.expensetracker.data.google.SheetValues.asText
import com.shashi.expensetracker.data.google.SheetValues.cell
import com.shashi.expensetracker.data.google.SheetValues.text
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey
import java.util.Locale

/** Turns Categories rows into [Category]s and back (AD-7, AD-9). */
internal object CategoryRowMapping {

  val requiredHeaders = listOf(NAME)

  /**
   * DESIGN.md's eight Category editor swatches, light hex, in order: the five Category colours, then
   * gradient-start, gradient-end and inbox.
   */
  val swatches =
    listOf("#F59E0B", "#10B981", "#E11D48", "#3B82F6", "#A855F7", "#6D28D9", "#EC4899", "#4F46E5")

  /** The Material Symbols icon of a Category added by sync. */
  const val ADDED_ICON = "category"

  /** The Category in [row]; a blank Name is [ParsedRow.Unkeyed]. Color and Icon may be blank. */
  fun category(row: List<Any?>, columns: Map<String, Int>): ParsedRow<Category> {
    val name = text(cell(row, columns, NAME)) ?: return ParsedRow.Unkeyed
    return ParsedRow.Valid(
      Category(
        name = name,
        color = text(cell(row, columns, COLOR)).orEmpty(),
        icon = text(cell(row, columns, ICON)).orEmpty(),
      )
    )
  }

  /** The cells to write for [category], header name to `USER_ENTERED` value, all kept as text. */
  fun cells(category: Category): Map<String, String> =
    mapOf(NAME to asText(category.name), COLOR to asText(category.color), ICON to asText(category.icon))

  /**
   * New Categories for [names], to append to a tab that holds [existing]. Each takes the first
   * swatch no Category (existing or new) uses yet, or Food's `#F59E0B` once all eight are taken, and
   * the `category` icon.
   */
  fun newCategories(names: List<String>, existing: List<Category>): List<Category> {
    val used = existing.mapTo(HashSet()) { it.color.trim().uppercase(Locale.ROOT) }
    return names
      .distinctBy(CategoryKey::of)
      .map { name ->
        val color = swatches.firstOrNull { it !in used } ?: swatches.first()
        used += color
        Category(name = name.trim(), color = color, icon = ADDED_ICON)
      }
  }
}
