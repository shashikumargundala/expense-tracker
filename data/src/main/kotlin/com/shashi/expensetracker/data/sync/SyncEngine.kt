package com.shashi.expensetracker.data.sync

import android.util.Log
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.ExpenseSheetResult
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.inbox.Dismissal
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * One sync run (AD-4), the only caller of [ExpenseSheetGateway]'s repair, tab reads and writes. It
 * applies only `UpsertExpense` and `DeleteExpense` entries; entries of the other five types stay in
 * the outbox for later stories.
 *
 * 0. Checks the Sheet still exists and isn't trashed: a trashed Sheet still accepts writes, so
 *    without this, entries would be "confirmed" into Drive's trash.
 * 1. Repairs the layout: adds any missing tab or header (AD-7).
 * 2. Reads all five tabs fresh.
 * 3. Repairs Expense IDs (AD-5, [ExpenseIdRepair]): writes each new UUID, `User` for a blank Source
 *    and each cleared SMS ID, then deletes retry duplicates bottom-up, and re-reads Expenses when a
 *    row moved or got a new ID.
 * 4. Pushes the Expense entries against that read ([ExpensePush]): each key's entries apply as one
 *    change, found by ID, never by a stored row index, and checked against the first entry's base.
 *    A row that differs from the base wins and the entries are dropped. Otherwise an upsert is
 *    written in place or appended, and the deletes go in one bottom-up request after the writes.
 *    A key's entries leave the outbox only after the API confirms its change.
 * 5. Re-reads Expenses when any row was written or deleted.
 * 6. Appends to Dismissed the SMS ID of every mirror Expense that is gone from the Sheet (AD-13):
 *    its ID is in neither a valid nor an invalid row, and it has no outbox entry left after the
 *    push. An SMS ID Dismissed already has is skipped.
 * 7. Appends to Categories every Category an Expense or Budget names that the tab lacks (AD-7).
 * 8. Refreshes the mirror ([MirrorRefresh]), with each Expense and Budget taking its Category's own
 *    spelling (AD-9). Mirror Expenses whose rows are invalid are kept.
 *
 * Any failed repair, read or write stops the run with the mirror unchanged; the next trigger
 * retries from a fresh read, so the ID repair is idempotent. Only the failure kind is logged.
 * [SyncResult.Synced.skipped] counts invalid rows across all five tabs.
 */
class SyncEngine
@Inject
constructor(
  private val sheets: ExpenseSheetGateway,
  private val expenses: ExpenseDao,
  private val outbox: OutboxDao,
  private val refreshMirror: MirrorRefresh,
  private val clock: Clock,
) {
  private val push = ExpensePush(sheets, expenses, outbox)

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

    val repair = ExpenseIdRepair.plan(sheetExpenses.rows)
    if (repair.fixes.isNotEmpty()) sheets.writeExpenseRepairs(sheetId, repair.fixes).failure()?.let { return it }
    if (repair.deletes.isNotEmpty()) sheets.deleteExpenseRows(sheetId, repair.deletes).failure()?.let { return it }
    if (repair.changesIds) sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }

    val pushed =
      when (val outcome = push(sheetId, sheetExpenses)) {
        is ExpensePush.Outcome.Stopped -> return checkNotNull(outcome.result.failure())
        is ExpensePush.Outcome.Done -> outcome.pushed
      }
    // Rows were appended, updated or deleted: refresh from what the Sheet now holds.
    if (pushed > 0) sheetExpenses = sheets.readExpenses(sheetId).rowsOr { return it }

    // After the push, so an entry it dropped no longer hides its Expense's SMS ID (AD-13).
    val vanished = vanishedDismissals(sheetExpenses, sheetDismissals)
    if (vanished.isNotEmpty()) sheets.appendDismissals(sheetId, vanished).failure()?.let { return it }

    val pulled =
      SheetContents(
        expenses = sheetExpenses.rows.mapNotNull { it.value.keyed },
        invalidExpenseIds = sheetExpenses.invalidIds(),
        categories = sheetCategories.values(),
        budgets = sheetBudgets.values(),
        dismissals = sheetDismissals.values() + vanished,
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

  /**
   * A Dismissal, dated now, for each SMS ID of a mirror Expense whose ID [read] no longer has, valid
   * or invalid, and that has no pending entry, unless [dismissed] already has it (AD-13). The mirror
   * is read before the outbox, so an Expense saved meanwhile is seen as pending, never as gone.
   */
  private suspend fun vanishedDismissals(
    read: TabReadResult.Rows<ExpenseRowRead>,
    dismissed: TabReadResult.Rows<Dismissal>,
  ): List<Dismissal> {
    val mirrored = expenses.getAll()
    val pending = outbox.rowKeys(OutboxType.expenseTypes).toHashSet()
    val inSheet = read.rows.mapNotNullTo(HashSet()) { it.value.id }
    val known = dismissed.rows.mapTo(HashSet()) { it.value.smsId }
    val now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
    return mirrored
      .filter { it.id !in inSheet && it.id !in pending }
      .mapNotNull { it.smsId?.takeIf(String::isNotBlank) }
      .filter { it !in known }
      .distinct()
      .sorted()
      .map { Dismissal(smsId = it, dismissedOn = now) }
  }

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

