package com.shashi.expensetracker.data.sync

import android.util.Log
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.expense.Expense

/**
 * The push of a sync run's Expense entries (AD-4), against one fresh read of the Expenses tab.
 *
 * The `UpsertExpense` and `DeleteExpense` entries of one key apply in outbox order as one change:
 * the last entry's type decides, and the first entry's base is the one checked, so several offline
 * edits of a pending key pass the check together. Keys apply in the order of their first entry.
 * Each key's row is found by ID in the read (the first row in sheet order), never by a stored index:
 * - **Base absent:** with no row it's a create, so the mirror's values are appended. With a row (a
 *   retry that already landed, or a row made in the Sheet) nothing is written.
 * - **Base present:** if the row is gone or differs from the base ([ExpenseBase.matches]), the Sheet
 *   wins and nothing is written. Otherwise an upsert writes the mirror's current values in place
 *   (never Source) and a delete deletes the row.
 * - A key whose only row is invalid (AD-7) is never written; its entries stay for a later run.
 * - An unreadable base counts as changed in the Sheet.
 *
 * Writes go first, then every delete in one bottom-up [ExpenseSheetGateway.deleteExpenseRows]: an
 * append lands below every row of the read and an update moves nothing, so the read's row numbers
 * still hold. A key's entries leave the outbox together, only once its change is confirmed or
 * found to need no write. The first failed write or delete stops the push and keeps the entries
 * not yet confirmed; a rerun recomputes from a fresh read.
 */
internal class ExpensePush(
  private val sheets: ExpenseSheetGateway,
  private val expenses: ExpenseDao,
  private val outbox: OutboxDao,
) {

  sealed interface Outcome {
    /** Every key was handled. [pushed] counts confirmed writes and deletes. */
    data class Done(val pushed: Int) : Outcome

    /** A write or delete wasn't confirmed; [result] is the gateway's reply. */
    data class Stopped(val result: RowWriteResult) : Outcome
  }

  /** One key's effective change. */
  sealed interface Change {
    /** Nothing to write: the Sheet wins, or the change already holds. */
    data object None : Change

    /** Write [expense] in place at [row], or append it when [row] is null. */
    data class Write(val expense: Expense, val row: Int?) : Change

    /** Delete [row]. */
    data class Delete(val row: Int) : Change
  }

  suspend operator fun invoke(sheetId: String, read: TabReadResult.Rows<ExpenseRowRead>): Outcome {
    val rowsById = read.firstRowsById()
    val invalidIds = read.invalidIds()
    val keys = outbox.getAll().filter { it.type in OutboxType.expenseTypes }.groupBy { it.rowKey }
    var pushed = 0
    val deletes = mutableListOf<Pair<List<OutboxEntity>, Int>>()
    for ((key, entries) in keys) {
      val row = rowsById[key]
      // Its only row is invalid, which is never written (AD-7); keep the entries for a later run.
      if (row == null && key in invalidIds) continue
      when (val change = change(entries, row) { expenses.get(key)?.toDomain() }) {
        Change.None -> remove(entries)
        is Change.Write -> {
          val written = sheets.writeExpense(sheetId, change.expense, change.row)
          if (written !is RowWriteResult.Written) return Outcome.Stopped(written)
          remove(entries)
          pushed++
        }
        is Change.Delete -> deletes += entries to change.row
      }
    }
    if (deletes.isNotEmpty()) {
      val deleted = sheets.deleteExpenseRows(sheetId, deletes.map { it.second })
      if (deleted !is RowWriteResult.Written) return Outcome.Stopped(deleted)
      deletes.forEach { remove(it.first) }
      pushed += deletes.size
    }
    return Outcome.Done(pushed)
  }

  private suspend fun remove(entries: List<OutboxEntity>) = entries.forEach { outbox.delete(it.id) }

  companion object {
    private const val TAG = "ExpensePush"

    /**
     * The effective change of one key's [entries] (in outbox order) against its valid Sheet [row]
     * in this run's read, or null when the read has none. [local] reads the mirror's current values;
     * an upsert with no mirror row left has nothing to push.
     */
    internal suspend fun change(
      entries: List<OutboxEntity>,
      row: SheetRow<ExpenseRowRead>?,
      local: suspend () -> Expense?,
    ): Change {
      val base =
        try {
          ExpenseBase.decode(entries.first().base)
        } catch (e: IllegalArgumentException) {
          Log.w(TAG, "Unreadable base; the Sheet wins")
          return Change.None
        }
      val sheetRow = row?.value?.expense
      val isDelete = entries.last().type == OutboxType.DeleteExpense
      if (base == null) {
        // A create: a row already there is a retry that landed, or the Sheet's own row.
        if (sheetRow != null || isDelete) return Change.None
        return local()?.let { Change.Write(it, null) } ?: Change.None
      }
      if (row == null || sheetRow == null || !base.matches(sheetRow)) return Change.None
      if (isDelete) return Change.Delete(row.row)
      return local()?.let { Change.Write(it, row.row) } ?: Change.None
    }
  }
}

/** Valid, keyed Expense ID to its row; a shared ID keeps the first row in sheet order (AD-5). */
internal fun TabReadResult.Rows<ExpenseRowRead>.firstRowsById(): Map<String, SheetRow<ExpenseRowRead>> {
  val byId = HashMap<String, SheetRow<ExpenseRowRead>>()
  rows.forEach { row -> row.value.keyed?.let { byId.putIfAbsent(it.id, row) } }
  return byId
}

/** The IDs of invalid Expenses rows (AD-7). */
internal fun TabReadResult.Rows<ExpenseRowRead>.invalidIds(): Set<String> =
  rows.filter { it.value.expense == null }.mapNotNullTo(HashSet()) { it.value.id }
