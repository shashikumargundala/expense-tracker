package com.shashi.expensetracker.data.settings

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SettingDao {
  @Query("SELECT * FROM settings") suspend fun getAll(): List<SettingEntity>

  @Upsert suspend fun upsert(settings: List<SettingEntity>)

  @Delete suspend fun delete(settings: List<SettingEntity>)
}
