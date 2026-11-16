package com.shashi.expensetracker.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
  @Insert suspend fun insert(entry: OutboxEntity): Long

  /** Every entry, in the order it was written. */
  @Query("SELECT * FROM outbox ORDER BY id") suspend fun getAll(): List<OutboxEntity>

  @Query("DELETE FROM outbox WHERE id = :id") suspend fun delete(id: Long)

  @Query("SELECT COUNT(*) FROM outbox") fun observeCount(): Flow<Int>

  /** The row keys with an entry of one of [types]. */
  @Query("SELECT DISTINCT row_key FROM outbox WHERE type IN (:types)")
  suspend fun rowKeys(types: List<OutboxType>): List<String>
}
