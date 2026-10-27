package com.shashi.expensetracker.domain.account

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Emits the stored [Session], or null when signed out. It returns a `Flow` instead of a sealed
 * result, and never throws: `:data` maps storage errors, and an unreadable session emits null, so
 * it behaves as signed out.
 */
class ObserveSessionUseCase @Inject constructor(private val sessions: SessionRepository) {
  operator fun invoke(): Flow<Session?> = sessions.observe()
}
