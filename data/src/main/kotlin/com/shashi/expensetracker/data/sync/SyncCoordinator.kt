package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.account.SessionRepository
import com.shashi.expensetracker.domain.sync.SyncController
import com.shashi.expensetracker.domain.sync.SyncState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * The one [SyncController]. It runs at most one [SyncEngine] run at a time behind a [Mutex]. For
 * now a request that arrives while one is running is dropped, not coalesced as AD-14 asks; entry 6
 * (sync runs on its own) adds coalescing. A run needs a stored session with an
 * Expense Sheet ID; without one the request does nothing. Runs happen in [scope], which outlives
 * any screen.
 */
@Singleton
class SyncCoordinator
@Inject
constructor(
  private val engine: SyncEngine,
  private val sessions: SessionRepository,
  private val outbox: OutboxDao,
  @SyncScope private val scope: CoroutineScope,
) : SyncController {

  private val mutex = Mutex()
  private val running = MutableStateFlow(false)

  override fun request() {
    scope.launch { runOnce() }
  }

  /** Runs one sync now. Returns null if one was already running or there is no Sheet to sync. */
  internal suspend fun runOnce(): SyncResult? {
    if (!mutex.tryLock()) return null
    try {
      val sheetId = sessions.observe().first()?.sheetId ?: return null
      running.value = true
      return engine.sync(sheetId)
    } finally {
      running.value = false
      mutex.unlock()
    }
  }

  override fun observeState(): Flow<SyncState> =
    combine(running, outbox.observeCount().catch { emit(0) }) { isRunning, pending ->
        SyncState(running = isRunning, pendingCount = pending)
      }
      .distinctUntilChanged()
}
