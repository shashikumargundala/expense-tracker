package com.shashi.expensetracker.domain.account

import kotlinx.coroutines.flow.Flow

/** The stored [Session]. Implemented in `:data`, which maps storage errors; nothing here throws. */
interface SessionRepository {
  /**
   * Emits the stored session, or null when no one is signed in, and again on every change. An
   * unreadable session emits null.
   */
  fun observe(): Flow<Session?>

  /** Stores [session], replacing any stored one. Returns false if storage failed. */
  suspend fun save(session: Session): Boolean
}
