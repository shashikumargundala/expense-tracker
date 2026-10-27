package com.shashi.expensetracker.data.google

import com.squareup.moshi.JsonClass

/**
 * A Drive v3 `File`, with only the fields the app reads or writes. Every field is optional so the
 * same type serves as a partial `files.update` body; Moshi omits nulls.
 */
@JsonClass(generateAdapter = true)
data class DriveFile(
  val id: String? = null,
  val name: String? = null,
  val mimeType: String? = null,
  /** RFC 3339 timestamp, for example `2026-10-28T09:15:00.000Z`. */
  val createdTime: String? = null,
  val appProperties: Map<String, String>? = null,
)

/** A Drive v3 `files.list` response. Paging isn't used: callers ask for a small `pageSize`. */
@JsonClass(generateAdapter = true)
data class FileList(val files: List<DriveFile> = emptyList())
