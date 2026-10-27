package com.shashi.expensetracker.data.account

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
  @Query("SELECT * FROM session WHERE id = ${SessionEntity.SINGLE_ROW_ID}")
  fun observe(): Flow<SessionEntity?>

  @Upsert suspend fun upsert(session: SessionEntity)
}
