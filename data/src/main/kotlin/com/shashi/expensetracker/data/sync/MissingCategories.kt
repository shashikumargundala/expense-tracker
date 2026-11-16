package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.data.google.CategoryRowMapping
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey

/** Categories that Expenses and Budgets name, matched trimmed and case-folded (AD-7, AD-9). */
internal object MissingCategories {

  /**
   * The Categories to append for every name an Expense or Budget of [sheet] uses that its
   * Categories lack, in sheet order (Expenses first) with the first spelling seen, each with its
   * colour and icon.
   */
  fun find(sheet: SheetContents): List<Category> {
    val known = sheet.categories.mapTo(HashSet()) { CategoryKey.of(it.name) }
    val names =
      (sheet.expenses.map { it.category } + sheet.budgets.map { it.category })
        .filter { CategoryKey.of(it) !in known }
        .distinctBy(CategoryKey::of)
    return CategoryRowMapping.newCategories(names, sheet.categories)
  }

  /**
   * [sheet] with each Expense and Budget whose Category matches one of its Categories using that
   * Category's own spelling. The first of two matching Categories wins, as in the mirror.
   */
  fun withCanonicalNames(sheet: SheetContents): SheetContents {
    val names = HashMap<String, String>()
    sheet.categories.forEach { names.putIfAbsent(CategoryKey.of(it.name), it.name) }
    fun canonical(name: String) = names[CategoryKey.of(name)] ?: name
    return sheet.copy(
      expenses = sheet.expenses.map { it.copy(category = canonical(it.category)) },
      budgets = sheet.budgets.map { it.copy(category = canonical(it.category)) },
    )
  }
}
