package com.shashi.expensetracker.domain.sync

import javax.inject.Inject

/** Asks the [SyncController] for a sync, for example on app open. Returns at once; never throws. */
class RequestSyncUseCase @Inject constructor(private val controller: SyncController) {
  operator fun invoke() = controller.request()
}
