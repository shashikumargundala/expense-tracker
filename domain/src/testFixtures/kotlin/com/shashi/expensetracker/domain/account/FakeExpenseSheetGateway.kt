package com.shashi.expensetracker.domain.account

/**
 * In-memory [ExpenseSheetGateway] shared by `:domain` and `:app` tests. Set [findResult] and
 * [createResult] to script outcomes; [findCalls] and [createCalls] count the calls.
 */
class FakeExpenseSheetGateway : ExpenseSheetGateway {
  var findResult: ExpenseSheetResult = ExpenseSheetResult.Ready("test-sheet")
  var createResult: ExpenseSheetResult = ExpenseSheetResult.Ready("created-sheet")
  var findCalls = 0
  var createCalls = 0

  override suspend fun find(): ExpenseSheetResult {
    findCalls++
    return findResult
  }

  override suspend fun create(): ExpenseSheetResult {
    createCalls++
    return createResult
  }
}
