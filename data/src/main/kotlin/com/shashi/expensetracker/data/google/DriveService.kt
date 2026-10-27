package com.shashi.expensetracker.data.google

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Drive REST v3, relative to [GoogleApiModule.DRIVE_BASE_URL]. Under `drive.file` it sees only files
 * the app created. Wrap calls in [googleApiCall]. Pass null to omit an optional query parameter.
 */
interface DriveService {
  /** `files.list`, for example `q = "appProperties has { key='expenseTracker' and value='v1' } and trashed=false"`. */
  @GET("files")
  suspend fun listFiles(
    @Query("q") q: String,
    @Query("orderBy") orderBy: String?,
    @Query("fields") fields: String?,
    @Query("pageSize") pageSize: Int?,
  ): FileList

  /** `files.update` metadata only, for example to set `appProperties`. */
  @PATCH("files/{fileId}")
  suspend fun updateFile(
    @Path("fileId") fileId: String,
    @Body metadata: DriveFile,
    @Query("fields") fields: String?,
  ): DriveFile
}
