package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.sync.SyncController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The app-wide scope sync runs in, so a run outlives the screen that asked for it. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SyncScope

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

  @Binds abstract fun bindSyncController(impl: SyncCoordinator): SyncController

  companion object {
    @Provides
    @Singleton
    @SyncScope
    fun provideSyncScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}
