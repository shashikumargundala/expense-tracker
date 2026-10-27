package com.shashi.expensetracker.data.google

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Sheets REST v4, relative to [GoogleApiModule.SHEETS_BASE_URL]. Wrap calls in [googleApiCall]. */
interface SheetsService {
  /** `spreadsheets.create`. Pass null [fields] for the full response. */
  @POST("spreadsheets")
  suspend fun createSpreadsheet(@Body spreadsheet: Spreadsheet, @Query("fields") fields: String?): Spreadsheet

  /** `spreadsheets.values.batchUpdate`. The response isn't read. */
  @POST("spreadsheets/{spreadsheetId}/values:batchUpdate")
  suspend fun batchUpdateValues(
    @Path("spreadsheetId") spreadsheetId: String,
    @Body request: BatchUpdateValuesRequest,
  )
}
