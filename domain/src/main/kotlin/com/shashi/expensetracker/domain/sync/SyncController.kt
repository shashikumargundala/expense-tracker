package com.shashi.expensetracker.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Starts syncs (AD-14). Implemented in `:data` by the one sync coordinator, which runs at most one
 * sync at a time. Neither call throws.
 */
interface SyncController {
  /**
   * Asks for a sync and returns at once. It does nothing while a sync is running or when no session
   * with an Expense Sheet is stored.
   */
  fun request()

  /** Emits the current [SyncState], and again on every change. */
  fun observeState(): Flow<SyncState>
}
