package com.shashi.expensetracker.data.account

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's one Room database. Version 1 holds only the session; later epics add tables with a
 * version bump and a migration. Schemas are exported to `data/schemas/`.
 */
@Database(entities = [SessionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
  abstract fun sessionDao(): SessionDao

  companion object {
    const val NAME = "expense_tracker.db"
  }
}
