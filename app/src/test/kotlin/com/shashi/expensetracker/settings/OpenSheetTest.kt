package com.shashi.expensetracker.settings

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class OpenSheetTest {

  private val app: Application = ApplicationProvider.getApplicationContext()
  private val sheetIntent =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/test-sheet/edit"))
      .setPackage(SHEETS_PACKAGE)
  private val marketIntent =
    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHEETS_PACKAGE"))
  private val webIntent =
    Intent(
      Intent.ACTION_VIEW,
      Uri.parse("https://play.google.com/store/apps/details?id=$SHEETS_PACKAGE"),
    )

  @Before
  fun setUp() {
    // Without this, Robolectric "starts" any intent; with it, unresolved intents throw.
    shadowOf(app).checkActivities(true)
  }

  private fun resolves(intent: Intent, packageName: String) {
    val info =
      ResolveInfo().apply {
        activityInfo =
          ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
          }
      }
    shadowOf(app.packageManager).addResolveInfoForIntent(intent, info)
  }

  private fun assertStarted(expected: Intent) {
    val started = shadowOf(app).nextStartedActivity
    assertEquals(expected.action, started.action)
    assertEquals(expected.data, started.data)
    assertEquals(expected.`package`, started.`package`)
  }

  @Test
  fun opensTheSheetInTheSheetsApp() {
    resolves(sheetIntent, SHEETS_PACKAGE)

    openSheetInSheetsApp(app, "test-sheet")

    assertStarted(sheetIntent)
    assertNull(shadowOf(app).nextStartedActivity)
  }

  @Test
  fun opensThePlayStoreWhenSheetsIsMissing() {
    resolves(marketIntent, "com.android.vending")

    openSheetInSheetsApp(app, "test-sheet")

    assertStarted(marketIntent)
    assertNull(shadowOf(app).nextStartedActivity)
  }

  @Test
  fun opensThePlayWebsiteWithoutThePlayStore() {
    resolves(webIntent, "com.android.browser")

    openSheetInSheetsApp(app, "test-sheet")

    assertStarted(webIntent)
    assertNull(shadowOf(app).nextStartedActivity)
  }

  @Test
  fun doesNothingWhenNothingResolves() {
    openSheetInSheetsApp(app, "test-sheet")

    assertNull(shadowOf(app).nextStartedActivity)
  }
}
