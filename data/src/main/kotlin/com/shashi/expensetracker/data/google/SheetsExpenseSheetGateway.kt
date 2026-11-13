package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.ExpenseSheetResult
import javax.inject.Inject

/**
 * [ExpenseSheetGateway] over Drive and Sheets REST. Find is `files.list` on the `appProperties`
 * marker with the oldest `createdTime` winning. Create writes tabs, headers and Categories first and
 * sets the marker last, so a half-built file is never found. Check is `files.get`: a 404 or a
 * trashed file is [ExpenseSheetResult.SheetMissing]. Nothing is logged here.
 */
class SheetsExpenseSheetGateway
@Inject
constructor(private val drive: DriveService, private val sheets: SheetsService) : ExpenseSheetGateway {

  override suspend fun find(): ExpenseSheetResult =
    when (
      val result =
        googleApiCall {
          drive.listFiles(
            q = ExpenseSheetLayout.FIND_QUERY,
            orderBy = "createdTime",
            fields = "files(id,createdTime)",
            pageSize = 100,
          )
        }
    ) {
      is ApiResult.Failure -> result.reason.toResult()
      is ApiResult.Success -> {
        val oldest =
          result.value.files
            .filter { !it.id.isNullOrBlank() }
            .minWithOrNull(compareBy(nullsLast()) { it.createdTime })
        oldest?.id?.let { ExpenseSheetResult.Ready(it) } ?: ExpenseSheetResult.NotFound
      }
    }

  override suspend fun create(): ExpenseSheetResult {
    val created =
      when (val r = googleApiCall { sheets.createSpreadsheet(ExpenseSheetLayout.newSpreadsheet(), "spreadsheetId") }) {
        is ApiResult.Failure -> return r.reason.toResult()
        is ApiResult.Success -> r.value.spreadsheetId
      }
    if (created.isNullOrBlank()) return ExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected)
    (googleApiCall { sheets.batchUpdateValues(created, ExpenseSheetLayout.initialValues()) } as? ApiResult.Failure)
      ?.let { return it.reason.toResult() }
    val marker = DriveFile(appProperties = mapOf(ExpenseSheetLayout.MARKER_KEY to ExpenseSheetLayout.MARKER_VALUE))
    (googleApiCall { drive.updateFile(created, marker, "id") } as? ApiResult.Failure)?.let {
      return it.reason.toResult()
    }
    return ExpenseSheetResult.Ready(created)
  }

  override suspend fun check(sheetId: String): ExpenseSheetResult =
    when (val result = googleApiCall { drive.getFile(sheetId, fields = "id,trashed") }) {
      is ApiResult.Failure ->
        if (result.reason == ApiFailure.Http(404)) ExpenseSheetResult.SheetMissing else result.reason.toResult()
      is ApiResult.Success ->
        if (result.value.trashed == true) ExpenseSheetResult.SheetMissing else ExpenseSheetResult.Ready(sheetId)
    }

  private fun ApiFailure.toResult(): ExpenseSheetResult =
    when (this) {
      is ApiFailure.NeedsUserConsent -> ExpenseSheetResult.NeedsConsent(request)
      is ApiFailure.Access -> ExpenseSheetResult.Failure(ExpenseSheetFailure.Access)
      ApiFailure.Network -> ExpenseSheetResult.Failure(ExpenseSheetFailure.Network)
      is ApiFailure.Http,
      ApiFailure.Unexpected -> ExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected)
    }
}
