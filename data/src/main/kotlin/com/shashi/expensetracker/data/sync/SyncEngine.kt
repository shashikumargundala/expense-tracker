package com.shashi.expensetracker.data.sync

import android.util.Log
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.ExpenseSheetResult
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.expense.Expense
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * One sync run (AD-4), the only caller of [ExpenseSheetGateway]'s tab reads and writes. This tracer
 * applies only `UpsertExpense` entries; entries of the other six types stay in the outbox for later
 * stories.
 *
 * 0. Checks the Sheet still exists and isn't trashed: a trashed Sheet still accepts writes, so
 *    without this, entries would be "confirmed" into Drive's trash.
 * 1. Reads the Expenses and Categories tabs fresh.
 * 2. For each `UpsertExpense` entry in order, finds the row by its ID in that read, never by a
 *    stored row index, and writes the Expense's mapped cells: an update in place when the ID is
 *    already a row (a retry), otherwise an append. The entry leaves the outbox only after the API
 *    confirms the write. The base is "absent" for every entry today, so there is no base check yet.
 * 3. Re-reads Expenses when anything was written, then refreshes the mirror ([MirrorRefresh]).
 *
 * Any failed read or write stops the run with the mirror unchanged; the next trigger retries. Only
 * the failure kind is logged.
 */
class SyncEngine
@Inject
constructor(
  private val sheets: ExpenseSheetGateway,
  private val expenses: ExpenseDao,
  private val outbox: OutboxDao,
  private val refreshMirror: MirrorRefresh,
) {

  suspend fun sync(sheetId: String): SyncResult {
    val result =
      try {
        run(sheetId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Sync storage error: ${e.javaClass.simpleName}")
        SyncResult.Failed(SyncFailure.Storage)
      }
    if (result is SyncResult.Failed) Log.w(TAG, "Sync stopped: ${result.reason}")
    return result
  }

  private suspend fun run(sheetId: String): SyncResult {
    when (val checked = sheets.check(sheetId)) {
      is ExpenseSheetResult.Ready -> Unit
      ExpenseSheetResult.SheetMissing -> return SyncResult.Failed(SyncFailure.SheetMissing)
      is ExpenseSheetResult.NeedsConsent -> return SyncResult.Failed(SyncFailure.NeedsConsent)
      is ExpenseSheetResult.Failure -> return SyncResult.Failed(checked.reason.toSyncFailure())
      ExpenseSheetResult.NotFound -> return SyncResult.Failed(SyncFailure.Unexpected)
    }
    var sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }
    val sheetCategories = sheets.readCategories(sheetId).rowsOr { return it }

    var rowsById = sheetExpenses.rowsById()
    var stale = false
    var pushed = 0
    for (entry in outbox.getAll()) {
      if (entry.type != OutboxType.UpsertExpense) continue
      val local = expenses.get(entry.rowKey)
      if (local == null) {
        // Nothing is left to push for this key.
        outbox.delete(entry.id)
        continue
      }
      if (stale && entry.rowKey !in rowsById) {
        // An earlier append didn't say which row it wrote; find rows by ID in a fresh read.
        sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }
        rowsById = sheetExpenses.rowsById()
        stale = false
      }
      when (val written = sheets.writeExpense(sheetId, local.toDomain(), rowsById[entry.rowKey])) {
        is RowWriteResult.Written -> {
          outbox.delete(entry.id)
          pushed++
          val row = written.row
          if (row != null) rowsById[entry.rowKey] = row else stale = true
        }
        is RowWriteResult.NeedsConsent -> return SyncResult.Failed(SyncFailure.NeedsConsent)
        RowWriteResult.SheetMissing -> return SyncResult.Failed(SyncFailure.SheetMissing)
        is RowWriteResult.Failure -> return SyncResult.Failed(written.reason.toSyncFailure())
      }
    }

    if (pushed > 0) sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }
    refreshMirror(sheetExpenses.rows.map { it.value }, sheetCategories.rows.map { it.value })
    return SyncResult.Synced(pushed = pushed, skipped = sheetExpenses.skipped + sheetCategories.skipped)
  }

  /** The successful read, or [onFailure] with the run's failure. */
  private inline fun <T> TabReadResult<T>.rowsOr(onFailure: (SyncResult.Failed) -> Nothing): TabReadResult.Rows<T> =
    when (this) {
      is TabReadResult.Rows -> this
      is TabReadResult.NeedsConsent -> onFailure(SyncResult.Failed(SyncFailure.NeedsConsent))
      TabReadResult.SheetMissing -> onFailure(SyncResult.Failed(SyncFailure.SheetMissing))
      is TabReadResult.Failure -> onFailure(SyncResult.Failed(reason.toSyncFailure()))
    }

  /** Expense ID to its row; a shared ID keeps the first row in sheet order (AD-5). */
  private fun TabReadResult.Rows<Expense>.rowsById(): MutableMap<String, Int> {
    val byId = HashMap<String, Int>()
    rows.forEach { byId.putIfAbsent(it.value.id, it.row) }
    return byId
  }

  private fun ExpenseSheetFailure.toSyncFailure(): SyncFailure =
    when (this) {
      ExpenseSheetFailure.Access -> SyncFailure.Access
      ExpenseSheetFailure.Network -> SyncFailure.Network
      ExpenseSheetFailure.Unexpected -> SyncFailure.Unexpected
      ExpenseSheetFailure.StorageFailed -> SyncFailure.Storage
    }

  private companion object {
    const val TAG = "SyncEngine"
  }
}

