package com.shashi.expensetracker.data.account

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shashi.expensetracker.data.budget.BudgetDao
import com.shashi.expensetracker.data.budget.BudgetEntity
import com.shashi.expensetracker.data.category.CategoryDao
import com.shashi.expensetracker.data.category.CategoryEntity
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.expense.ExpenseEntity
import com.shashi.expensetracker.data.inbox.DismissalDao
import com.shashi.expensetracker.data.inbox.DismissalEntity
import com.shashi.expensetracker.data.settings.SettingDao
import com.shashi.expensetracker.data.settings.SettingEntity
import com.shashi.expensetracker.data.sync.OutboxDao
import com.shashi.expensetracker.data.sync.OutboxEntity

/**
 * The app's one Room database. Version 1 held only the session; version 2 adds the whole mirror
 * (one table per Sheet tab) and the outbox (AD-3), by an auto-migration that only adds tables, so
 * the session is kept. Every schema change ships a migration; there is no destructive fallback.
 * Schemas are exported to `data/schemas/`.
 */
@Database(
  entities =
    [
      SessionEntity::class,
      ExpenseEntity::class,
      CategoryEntity::class,
      BudgetEntity::class,
      DismissalEntity::class,
      SettingEntity::class,
      OutboxEntity::class,
    ],
  version = 2,
  exportSchema = true,
  autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(MirrorConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun sessionDao(): SessionDao

  abstract fun expenseDao(): ExpenseDao

  abstract fun categoryDao(): CategoryDao

  abstract fun budgetDao(): BudgetDao

  abstract fun dismissalDao(): DismissalDao

  abstract fun settingDao(): SettingDao

  abstract fun outboxDao(): OutboxDao

  companion object {
    const val NAME = "expense_tracker.db"
  }
}
