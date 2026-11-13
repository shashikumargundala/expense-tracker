package com.shashi.expensetracker.domain.account

/**
 * In-memory [ExpenseSheetGateway] shared by `:domain` and `:app` tests. Set [findResult],
 * [createResult] and [checkResult] to script outcomes; [findCalls], [createCalls] and [checkCalls]
 * count the calls, and [checkedIds] records each checked Sheet ID.
 */
class FakeExpenseSheetGateway : ExpenseSheetGateway {
  var findResult: ExpenseSheetResult = ExpenseSheetResult.Ready("test-sheet")
  var createResult: ExpenseSheetResult = ExpenseSheetResult.Ready("created-sheet")

  /** What `check` returns; null means `Ready` with the checked ID. */
  var checkResult: ExpenseSheetResult? = null
  var findCalls = 0
  var createCalls = 0
  var checkCalls = 0
  val checkedIds = mutableListOf<String>()

  override suspend fun find(): ExpenseSheetResult {
    findCalls++
    return findResult
  }

  override suspend fun create(): ExpenseSheetResult {
    createCalls++
    return createResult
  }

  override suspend fun check(sheetId: String): ExpenseSheetResult {
    checkCalls++
    checkedIds += sheetId
    return checkResult ?: ExpenseSheetResult.Ready(sheetId)
  }
}
