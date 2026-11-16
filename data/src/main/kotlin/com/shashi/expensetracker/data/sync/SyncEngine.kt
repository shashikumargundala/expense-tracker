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
 * One sync run (AD-4), the only caller of [ExpenseSheetGateway]'s repair, tab reads and writes. It
 * applies only `UpsertExpense` entries; entries of the other six types stay in the outbox for later
 * stories.
 *
 * 0. Checks the Sheet still exists and isn't trashed: a trashed Sheet still accepts writes, so
 *    without this, entries would be "confirmed" into Drive's trash.
 * 1. Repairs the layout: adds any missing tab or header (AD-7).
 * 2. Reads all five tabs fresh.
 * 3. For each `UpsertExpense` entry in order, finds the row by its ID in that read, never by a
 *    stored row index, and writes the Expense's mapped cells: an update in place when the ID is
 *    already a row (a retry), otherwise an append. The entry leaves the outbox only after the API
 *    confirms the write. The base is "absent" for every entry today, so there is no base check yet.
 * 4. Re-reads Expenses when anything was written.
 * 5. Appends to Categories every Category an Expense or Budget names that the tab lacks (AD-7).
 * 6. Refreshes the mirror ([MirrorRefresh]), with each Expense and Budget taking its Category's own
 *    spelling (AD-9).
 *
 * Any failed repair, read or write stops the run with the mirror unchanged; the next trigger
 * retries. Only the failure kind is logged. [SyncResult.Synced.skipped] counts invalid rows across
 * all five tabs.
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
    sheets.repairLayout(sheetId).failure()?.let { return it }
    var sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }
    val sheetCategories = sheets.readCategories(sheetId).rowsOr { return it }
    val sheetBudgets = sheets.readBudgets(sheetId).rowsOr { return it }
    val sheetDismissals = sheets.readDismissals(sheetId).rowsOr { return it }
    val sheetSettings = sheets.readSettings(sheetId).rowsOr { return it }

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
      val written = sheets.writeExpense(sheetId, local.toDomain(), rowsById[entry.rowKey])
      written.failure()?.let { return it }
      outbox.delete(entry.id)
      pushed++
      val row = (written as RowWriteResult.Written).row
      if (row != null) rowsById[entry.rowKey] = row else stale = true
    }
    if (pushed > 0) sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }

    val pulled =
      SheetContents(
        expenses = sheetExpenses.values(),
        categories = sheetCategories.values(),
        budgets = sheetBudgets.values(),
        dismissals = sheetDismissals.values(),
        settings = sheetSettings.values(),
      )
    val added = MissingCategories.find(pulled)
    if (added.isNotEmpty()) sheets.appendCategories(sheetId, added).failure()?.let { return it }
    refreshMirror(MissingCategories.withCanonicalNames(pulled.copy(categories = pulled.categories + added)))
    val skipped =
      sheetExpenses.skipped + sheetCategories.skipped + sheetBudgets.skipped + sheetDismissals.skipped +
        sheetSettings.skipped
    return SyncResult.Synced(pushed = pushed, skipped = skipped)
  }

  private fun <T> TabReadResult.Rows<T>.values(): List<T> = rows.map { it.value }

  /** The run's failure for a write that wasn't confirmed, or null once it was. */
  private fun RowWriteResult.failure(): SyncResult.Failed? =
    when (this) {
      is RowWriteResult.Written -> null
      is RowWriteResult.NeedsConsent -> SyncResult.Failed(SyncFailure.NeedsConsent)
      RowWriteResult.SheetMissing -> SyncResult.Failed(SyncFailure.SheetMissing)
      is RowWriteResult.Failure -> SyncResult.Failed(reason.toSyncFailure())
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

