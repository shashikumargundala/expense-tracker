package com.shashi.expensetracker.data.account

import android.util.Log
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [SessionRepository] over the Room `session` table. Room errors are caught here: an unreadable
 * session emits null (signed out) and a failed save returns false. Only the exception class is
 * logged.
 */
class RoomSessionRepository @Inject constructor(private val dao: SessionDao) : SessionRepository {

  override fun observe(): Flow<Session?> =
    dao
      .observe()
      .map { entity ->
        entity?.let { Session(accountId = it.accountId, email = it.email, sheetId = it.sheetId) }
      }
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w(TAG, "Session read failed: ${e.javaClass.simpleName}")
        emit(null)
      }

  override suspend fun save(session: Session): Boolean =
    try {
      dao.upsert(
        SessionEntity(accountId = session.accountId, email = session.email, sheetId = session.sheetId)
      )
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Session save failed: ${e.javaClass.simpleName}")
      false
    }

  private companion object {
    const val TAG = "SessionRepository"
  }
}
