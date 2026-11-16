package com.shashi.expensetracker.data.account

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.shashi.expensetracker.data.BuildConfig
import com.shashi.expensetracker.data.budget.BudgetDao
import com.shashi.expensetracker.data.category.CategoryDao
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.inbox.DismissalDao
import com.shashi.expensetracker.data.settings.SettingDao
import com.shashi.expensetracker.data.sync.OutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

/** The Web OAuth client ID Credential Manager uses as the server client ID. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class GoogleWebClientId

/**
 * Provides the Room database, its DAOs, the build's Web client ID and the resumed-activity tracker.
 * Tests keep this module and replace only [AccountModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object AccountDataModule {

  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

  @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

  @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

  @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

  @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

  @Provides fun provideDismissalDao(db: AppDatabase): DismissalDao = db.dismissalDao()

  @Provides fun provideSettingDao(db: AppDatabase): SettingDao = db.settingDao()

  @Provides fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

  /** `ExpenseTrackerApp` registers every callback in this set at startup. */
  @Provides
  @IntoSet
  fun provideActivityTracker(tracker: ResumedActivityTracker): Application.ActivityLifecycleCallbacks =
    tracker

  @Provides @GoogleWebClientId fun provideGoogleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
}
