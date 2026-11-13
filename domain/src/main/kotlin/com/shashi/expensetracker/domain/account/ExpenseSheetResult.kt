package com.shashi.expensetracker.domain.account

/** Outcome of an [ExpenseSheetGateway] call. */
sealed interface ExpenseSheetResult {
  /** The Sheet exists and [sheetId] identifies it. */
  data class Ready(val sheetId: String) : ExpenseSheetResult

  /** From `find()` only: no tagged Sheet exists. */
  data object NotFound : ExpenseSheetResult

  /** The user must grant Drive access first; [request] goes back to `AccountGateway.requestConsent`. */
  data class NeedsConsent(val request: ConsentRequest) : ExpenseSheetResult

  data class Failure(val reason: ExpenseSheetFailure) : ExpenseSheetResult
}

/** Why finding or creating the Sheet failed. */
enum class ExpenseSheetFailure {
  /** Access was refused or is unavailable (no session, 401 after retry, no Play services). */
  Access,

  /** No response: offline, timeout or a dropped connection. */
  Network,

  /** Any other error response or unreadable reply. */
  Unexpected,

  /** The session could not be stored once the Sheet was known. */
  StorageFailed,
}
