package com.shashi.expensetracker.data.inbox

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DismissalDao {
  @Query("SELECT * FROM dismissed") suspend fun getAll(): List<DismissalEntity>

  @Upsert suspend fun upsert(dismissals: List<DismissalEntity>)

  @Delete suspend fun delete(dismissals: List<DismissalEntity>)
}
