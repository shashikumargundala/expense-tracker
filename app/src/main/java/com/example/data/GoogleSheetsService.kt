package com.example.data

import retrofit2.http.*

interface GoogleSheetsService {

    @POST("v4/spreadsheets")
    suspend fun createSpreadsheet(
        @Header("Authorization") authHeader: String,
        @Body body: CreateSpreadsheetRequest
    ): CreateSpreadsheetResponse

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getSpreadsheetValues(
        @Header("Authorization") authHeader: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String
    ): ValueRangeResponse

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendSpreadsheetValues(
        @Header("Authorization") authHeader: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "USER_ENTERED",
        @Query("insertDataOption") insertDataOption: String = "INSERT_ROWS",
        @Body body: ValueRangeRequest
    ): AppendValuesResponse

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun updateSpreadsheetValues(
        @Header("Authorization") authHeader: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "USER_ENTERED",
        @Body body: ValueRangeRequest
    ): UpdateValuesResponse
}

data class CreateSpreadsheetRequest(
    val properties: SpreadsheetProperties,
    val sheets: List<SheetRequest>? = null
)

data class SpreadsheetProperties(
    val title: String
)

data class SheetRequest(
    val properties: SheetProperties
)

data class SheetProperties(
    val title: String
)

data class CreateSpreadsheetResponse(
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val properties: SpreadsheetProperties
)

data class ValueRangeResponse(
    val range: String,
    val majorDimension: String,
    val values: List<List<String>>?
)

data class ValueRangeRequest(
    val range: String,
    val majorDimension: String = "ROWS",
    val values: List<List<Any>>
)

data class AppendValuesResponse(
    val spreadsheetId: String,
    val tableRange: String,
    val updates: UpdateDetailsResponse
)

data class UpdateValuesResponse(
    val spreadsheetId: String,
    val updatedRange: String,
    val updatedRows: Int,
    val updatedColumns: Int,
    val updatedCells: Int
)

data class UpdateDetailsResponse(
    val spreadsheetId: String,
    val updatedRange: String,
    val updatedRows: Int,
    val updatedColumns: Int,
    val updatedCells: Int
)
