package com.shashi.expensetracker.data.google

import retrofit2.http.Body
import retrofit2.http.GET
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

  /**
   * `spreadsheets.values.get` for [range] in A1 notation (`Expenses`, `Expenses!1:1`). Sync reads
   * with `valueRenderOption=UNFORMATTED_VALUE` and `dateTimeRenderOption=SERIAL_NUMBER` (AD-7).
   */
  @GET("spreadsheets/{spreadsheetId}/values/{range}")
  suspend fun getValues(
    @Path("spreadsheetId") spreadsheetId: String,
    @Path("range") range: String,
    @Query("valueRenderOption") valueRenderOption: String?,
    @Query("dateTimeRenderOption") dateTimeRenderOption: String?,
  ): ValueRange

  /**
   * `spreadsheets.values.append`: adds [body]'s rows after the table that [range] finds. Sync passes
   * `valueInputOption=USER_ENTERED` and `insertDataOption=INSERT_ROWS`.
   */
  @POST("spreadsheets/{spreadsheetId}/values/{range}:append")
  suspend fun appendValues(
    @Path("spreadsheetId") spreadsheetId: String,
    @Path("range") range: String,
    @Body body: ValueRange,
    @Query("valueInputOption") valueInputOption: String,
    @Query("insertDataOption") insertDataOption: String,
  ): AppendValuesResponse
}
