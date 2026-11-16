package com.shashi.expensetracker.domain.sync

/** Whether a sync is [running], and how many outbox entries are waiting ([pendingCount]). */
data class SyncState(val running: Boolean = false, val pendingCount: Int = 0)
