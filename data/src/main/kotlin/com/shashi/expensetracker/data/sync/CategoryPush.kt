package com.shashi.expensetracker.data.sync

import android.util.Log
import com.shashi.expensetracker.data.category.CategoryDao
import com.shashi.expensetracker.data.category.toDomain
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.SheetTab
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey

/**
 * The push of a sync run's Category entries (AD-4, AD-9), against one fresh read of the Expenses,
 * Categories and Budgets tabs, after the Expense push.
 *
 * The `UpsertCategory` and `DeleteCategory` entries of one [CategoryKey] apply in outbox order as
 * one change: the last entry's type decides, and the first entry's base is the one checked. Keys
 * apply in the order of their first entry. Each key's row is the first Categories row in sheet
 * order with that key:
 * - **Upsert, base absent:** with no row the mirror's Category is appended; with a row nothing is
 *   written.
 * - **Upsert, base present:** if the row is gone or differs from the base ([CategoryBase.matches])
 *   the Sheet wins and nothing is written. Otherwise the mirror's Name, Color and Icon are written
 *   in place.
 * - **Upsert, invalid row:** a key whose only row is invalid (AD-7) is never written or appended;
 *   its entries stay for a later run. The real Categories mapping never finds a row invalid today,
 *   so only a fake gateway reaches this; it is kept for a mapping that adds validation.
 * - **Delete:** no base check. Every Expenses row whose Category cell has the key, valid or invalid,
 *   has that cell rewritten to the entry's `reassignTo` (one write per key), then every Budgets row
 *   of the key, valid or invalid, and every Categories row of the key is deleted. When `reassignTo` is itself deleted in this run, the rows
 *   go on to its own `reassignTo`.
 * - An unreadable base, or a delete with no `reassignTo`, counts as changed in the Sheet.
 *
 * Writes go first; then all Budgets deletes in one bottom-up request, then all Categories deletes
 * in one, so the read's row numbers still hold. A key's entries leave only once its change is
 * confirmed or found to need no write. The first failed write or delete stops the push and keeps
 * the entries not yet confirmed; a rerun recomputes from a fresh read.
 */
internal class CategoryPush(
  private val sheets: ExpenseSheetGateway,
  private val categories: CategoryDao,
  private val outbox: OutboxDao,
) {

  /** One key's effective change. */
  sealed interface Change {
    /** Nothing to write: the Sheet wins, or the change already holds. */
    data object None : Change

    /** The key's only row is invalid: write nothing and keep the entries. */
    data object Keep : Change

    /** Write [category] in place at [row], or append it when [row] is null. */
    data class Write(val category: Category, val row: Int?) : Change

    /** Delete the Category, moving its Expenses to [reassignTo]. */
    data class Delete(val reassignTo: String) : Change
  }

  /** A confirmed or pending delete: its entries, its Budgets rows and its Categories rows. */
  private class PendingDelete(val entries: List<OutboxEntity>, val budgetRows: List<Int>, val categoryRows: List<Int>)

  suspend operator fun invoke(
    sheetId: String,
    expenseRead: TabReadResult.Rows<ExpenseRowRead>,
    categoryRead: TabReadResult.Rows<Category>,
    budgetRead: TabReadResult.Rows<Budget>,
  ): ExpensePush.Outcome {
    val rowsByKey = HashMap<String, SheetRow<Category>>()
    categoryRead.rows.forEach { rowsByKey.putIfAbsent(CategoryKey.of(it.value.name), it) }
    val invalidKeys = categoryRead.invalidKeys.mapTo(HashSet(), CategoryKey::of)
    val keys =
      outbox.getAll().filter { it.type in OutboxType.categoryTypes }.groupBy { CategoryKey.of(it.rowKey) }
    val changes =
      keys.mapValues { (key, entries) ->
        change(entries, rowsByKey[key], key in invalidKeys) { categories.get(entries.last().rowKey.trim())?.toDomain() }
      }
    val reassign = changes.mapNotNull { (key, c) -> (c as? Change.Delete)?.let { key to it.reassignTo } }.toMap()
    var pushed = 0
    val deletes = mutableListOf<PendingDelete>()
    for ((key, entries) in keys) {
      when (val change = changes.getValue(key)) {
        Change.Keep -> Unit
        Change.None -> remove(entries)
        is Change.Write -> {
          val written = sheets.writeCategory(sheetId, change.category, change.row)
          if (written !is RowWriteResult.Written) return ExpensePush.Outcome.Stopped(written)
          remove(entries)
          pushed++
        }
        is Change.Delete -> {
          val expenseRows =
            expenseRead.rows.filter { row -> row.value.category?.let { CategoryKey.of(it) == key } == true }.map { it.row }
          if (expenseRows.isNotEmpty()) {
            val written = sheets.writeExpenseCategories(sheetId, expenseRows, finalTarget(key, reassign))
            if (written !is RowWriteResult.Written) return ExpensePush.Outcome.Stopped(written)
          }
          deletes +=
            PendingDelete(
              entries,
              budgetRead.rows.filter { CategoryKey.of(it.value.category) == key }.map { it.row } +
                budgetRead.invalidRows.filter { CategoryKey.of(it.value.substringAfter(BUDGET_KEY_SEPARATOR)) == key }.map { it.row },
              categoryRead.rows.filter { CategoryKey.of(it.value.name) == key }.map { it.row },
            )
        }
      }
    }
    if (deletes.isNotEmpty()) {
      for ((tab, rows) in listOf(SheetTab.Budgets to deletes.flatMap { it.budgetRows }, SheetTab.Categories to deletes.flatMap { it.categoryRows })) {
        if (rows.isEmpty()) continue
        val deleted = sheets.deleteRows(sheetId, tab, rows)
        if (deleted !is RowWriteResult.Written) return ExpensePush.Outcome.Stopped(deleted)
      }
      deletes.forEach { remove(it.entries) }
      pushed += deletes.size
    }
    return ExpensePush.Outcome.Done(pushed)
  }

  private suspend fun remove(entries: List<OutboxEntity>) = entries.forEach { outbox.delete(it.id) }

  companion object {
    private const val TAG = "CategoryPush"

    /** A Budget's row key is `yyyy-MM|Category` (AD-3); the Month never holds this separator. */
    private const val BUDGET_KEY_SEPARATOR = '|'

    /**
     * Where the Expenses of the deleted [key] end up: its `reassignTo`, followed while that is also
     * deleted in this run ([reassign], key to `reassignTo`). A cycle stops at the last name seen.
     */
    internal fun finalTarget(key: String, reassign: Map<String, String>): String {
      var target = reassign.getValue(key)
      val seen = hashSetOf(key)
      while (true) {
        val next = CategoryKey.of(target)
        if (!seen.add(next)) return target
        target = reassign[next] ?: return target
      }
    }

    /**
     * The effective change of one key's [entries] (in outbox order) against its first Categories
     * [row] in this run's read, or null when the read has none. [invalid] is whether the read has an
     * invalid row with the key. [local] reads the mirror's current Category; an upsert with no mirror
     * row left has nothing to push.
     */
    internal suspend fun change(
      entries: List<OutboxEntity>,
      row: SheetRow<Category>?,
      invalid: Boolean,
      local: suspend () -> Category?,
    ): Change {
      val last = entries.last()
      if (last.type == OutboxType.DeleteCategory) {
        val reassignTo = last.payload?.takeIf { it.isNotBlank() }
        if (reassignTo == null) {
          Log.w(TAG, "Delete with no reassign target; the Sheet wins")
          return Change.None
        }
        return Change.Delete(reassignTo)
      }
      if (row == null && invalid) return Change.Keep
      val base =
        try {
          CategoryBase.decode(entries.first().base)
        } catch (e: IllegalArgumentException) {
          Log.w(TAG, "Unreadable base; the Sheet wins")
          return Change.None
        }
      if (base == null) {
        // A create: a row already there is a retry that landed, or the Sheet's own row.
        if (row != null) return Change.None
        return local()?.let { Change.Write(it, null) } ?: Change.None
      }
      if (row == null || !base.matches(row.value)) return Change.None
      return local()?.let { Change.Write(it, row.row) } ?: Change.None
    }
  }
}
