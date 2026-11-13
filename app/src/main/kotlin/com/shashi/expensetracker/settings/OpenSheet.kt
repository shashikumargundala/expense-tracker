package com.shashi.expensetracker.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Package of the Google Sheets app, the only app Open my Sheet targets. */
const val SHEETS_PACKAGE = "com.google.android.apps.docs.editors.sheets"

/** The web URL of the Sheet with [sheetId], which the Sheets app opens. */
fun sheetUrl(sheetId: String): String = "https://docs.google.com/spreadsheets/d/$sheetId/edit"

/**
 * Opens the Expense Sheet [sheetId] in the Google Sheets app. When Sheets isn't installed it opens
 * the Sheets listing in the Play Store, then on the Play website; if nothing can open any of them,
 * nothing happens. A step that throws `ActivityNotFoundException` or `SecurityException` fails
 * and the next is tried. Each step is a plain start in a try/catch, so no `<queries>` entry is
 * needed. Never logs the URL or the ID.
 */
fun openSheetInSheetsApp(context: Context, sheetId: String) {
  val attempts =
    sequenceOf(
      { Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl(sheetId))).setPackage(SHEETS_PACKAGE) },
      { Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHEETS_PACKAGE")) },
      {
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/apps/details?id=$SHEETS_PACKAGE"),
        )
      },
    )
  attempts.firstOrNull { intent -> context.tryStartActivity(intent()) }
}

private fun Context.tryStartActivity(intent: Intent): Boolean =
  try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
  } catch (_: ActivityNotFoundException) {
    false
  } catch (_: SecurityException) {
    false
  }
