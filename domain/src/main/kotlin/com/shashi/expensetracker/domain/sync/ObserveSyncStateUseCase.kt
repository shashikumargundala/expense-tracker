package com.shashi.expensetracker.domain.sync

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Emits the current [SyncState]. Never throws. */
class ObserveSyncStateUseCase @Inject constructor(private val controller: SyncController) {
  operator fun invoke(): Flow<SyncState> = controller.observeState()
}
