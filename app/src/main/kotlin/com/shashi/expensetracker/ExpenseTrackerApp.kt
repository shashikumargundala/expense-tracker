package com.shashi.expensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApp : Application() {

  /** Bound in `:data`, for example the resumed-activity tracker Credential Manager needs. */
  @Inject lateinit var activityCallbacks: Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>

  override fun onCreate() {
    super.onCreate()
    activityCallbacks.forEach(::registerActivityLifecycleCallbacks)
  }
}
