package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.TabReadResult

/** Sheets answers 404 for a deleted spreadsheet. */
private const val NOT_FOUND = 404

/** An [ApiFailure] from a tab read as the gateway's [TabReadResult]. */
internal fun ApiFailure.toReadResult(): TabReadResult<Nothing> =
  when (this) {
    is ApiFailure.NeedsUserConsent -> TabReadResult.NeedsConsent(request)
    is ApiFailure.Access -> TabReadResult.Failure(ExpenseSheetFailure.Access)
    ApiFailure.Network -> TabReadResult.Failure(ExpenseSheetFailure.Network)
    ApiFailure.Http(NOT_FOUND) -> TabReadResult.SheetMissing
    is ApiFailure.Http,
    ApiFailure.Unexpected -> TabReadResult.Failure(ExpenseSheetFailure.Unexpected)
  }

/** An [ApiFailure] from a write (or a read that a write needs) as the gateway's [RowWriteResult]. */
internal fun ApiFailure.toWriteResult(): RowWriteResult =
  when (val read = toReadResult()) {
    is TabReadResult.NeedsConsent -> RowWriteResult.NeedsConsent(read.request)
    is TabReadResult.Failure -> RowWriteResult.Failure(read.reason)
    TabReadResult.SheetMissing -> RowWriteResult.SheetMissing
    is TabReadResult.Rows -> RowWriteResult.Failure(ExpenseSheetFailure.Unexpected)
  }
