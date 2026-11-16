package com.shashi.expensetracker.data.sync

/** Outcome of one [SyncEngine] run. */
sealed interface SyncResult {
  /**
   * Every entry this engine applies was pushed and the mirror was refreshed. [pushed] counts the
   * confirmed writes; [skipped] counts Sheet rows that couldn't be read (AD-7).
   */
  data class Synced(val pushed: Int, val skipped: Int) : SyncResult

  /** The run stopped. Entries not yet confirmed stay in the outbox and the mirror is unchanged. */
  data class Failed(val reason: SyncFailure) : SyncResult
}

/** Why a sync run stopped. The next trigger retries; none of these prompts or crashes. */
enum class SyncFailure {
  /** Offline, a timeout or a dropped connection. */
  Network,

  /** Access was refused or unavailable. */
  Access,

  /** Drive access needs the user's consent, which background sync never asks for (AD-10). */
  NeedsConsent,

  /** The Expense Sheet is gone (AD-20). */
  SheetMissing,

  /** An unreadable tab or any other error reply. */
  Unexpected,

  /** The mirror or outbox couldn't be read or written. */
  Storage,
}
