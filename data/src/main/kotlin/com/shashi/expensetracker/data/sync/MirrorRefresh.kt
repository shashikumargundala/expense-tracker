package com.shashi.expensetracker.data.sync

import androidx.room.withTransaction
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.category.CategoryDao
import com.shashi.expensetracker.data.category.toDomain
import com.shashi.expensetracker.data.category.toEntity
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import javax.inject.Inject

/**
 * Applies a successful read of the Sheet to the mirror as one atomic diff (AD-18). Inside one Room
 * transaction it reads the outbox, then upserts only rows that changed or are new and deletes only
 * rows missing from the Sheet. A row with a pending outbox entry is left exactly as it is, so a
 * change saved during the run is never overwritten or erased. No table is cleared and refilled.
 */
class MirrorRefresh
@Inject
constructor(
  private val db: AppDatabase,
  private val expenses: ExpenseDao,
  private val categories: CategoryDao,
  private val outbox: OutboxDao,
) {

  /** [sheetExpenses] and [sheetCategories] are the tabs' rows in sheet order. Throws on a Room error. */
  suspend operator fun invoke(sheetExpenses: List<Expense>, sheetCategories: List<Category>) {
    db.withTransaction {
      refreshExpenses(sheetExpenses)
      refreshCategories(sheetCategories)
    }
  }

  private suspend fun refreshExpenses(sheet: List<Expense>) {
    val pending = outbox.rowKeys(OutboxType.expenseTypes).toSet()
    // A shared ID keeps the first row in sheet order (AD-5); the repair itself comes later.
    val pulled = sheet.distinctBy { it.id }
    val current = expenses.getAll().associateBy { it.id }
    val changed = pulled.filter { it.id !in pending && current[it.id]?.toDomain() != it }
    if (changed.isNotEmpty()) expenses.upsertAll(changed.map { it.toEntity() })
    val inSheet = pulled.mapTo(HashSet()) { it.id }
    val missing = current.keys.filter { it !in inSheet && it !in pending }
    missing.chunked(MAX_ARGS).forEach { expenses.deleteByIds(it) }
  }

  private suspend fun refreshCategories(sheet: List<Category>) {
    // Category names compare trimmed and case-folded (AD-9).
    fun key(name: String) = name.trim().lowercase()
    val pending = outbox.rowKeys(OutboxType.categoryTypes).mapTo(HashSet(), ::key)
    val pulled = sheet.distinctBy { key(it.name) }
    val current = categories.getAll().associateBy { key(it.name) }
    val changed = pulled.filter { key(it.name) !in pending && current[key(it.name)]?.toDomain() != it }
    if (changed.isNotEmpty()) categories.upsertAll(changed.map { it.toEntity() })
    val inSheet = pulled.mapTo(HashSet()) { key(it.name) }
    val missing = current.filterKeys { it !in inSheet && it !in pending }.values.map { it.name }
    missing.chunked(MAX_ARGS).forEach { categories.deleteByNames(it) }
  }

  private companion object {
    /** Stays under SQLite's bound-argument limit. */
    const val MAX_ARGS = 500
  }
}
