package com.shashi.expensetracker.data.account

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the currently resumed [Activity], weakly, so Credential Manager can show its account
 * picker without an Android type in the `:domain` [com.shashi.expensetracker.domain.account.AccountGateway].
 *
 * It is bound into the set of [Application.ActivityLifecycleCallbacks] that `ExpenseTrackerApp`
 * registers at startup, so it sees the first resume.
 */
@Singleton
class ResumedActivityTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

  @Volatile private var resumed: WeakReference<Activity>? = null

  /** The resumed activity, or null when none is in front. */
  val current: Activity?
    get() = resumed?.get()

  override fun onActivityResumed(activity: Activity) {
    resumed = WeakReference(activity)
  }

  override fun onActivityPaused(activity: Activity) {
    if (resumed?.get() === activity) resumed = null
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

  override fun onActivityStarted(activity: Activity) = Unit

  override fun onActivityStopped(activity: Activity) = Unit

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

  override fun onActivityDestroyed(activity: Activity) {
    if (resumed?.get() === activity) resumed = null
  }
}
