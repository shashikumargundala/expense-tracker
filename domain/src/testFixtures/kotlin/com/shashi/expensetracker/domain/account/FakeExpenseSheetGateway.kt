package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense

/**
 * In-memory [ExpenseSheetGateway] shared by `:domain`, `:data` and `:app` tests. Set [findResult],
 * [createResult] and [checkResult] to script outcomes; [findCalls], [createCalls] and [checkCalls]
 * count the calls, and [checkedIds] records each checked Sheet ID.
 *
 * The Expenses and Categories tabs are [expenses] and [categories], in sheet order, under a header in
 * row 1 (so the first Expense is row 2). By default reads return them and writes succeed; set
 * [readExpensesResult], [readCategoriesResult] or [writeResult] to script a failure instead. Every
 * write is recorded in [writes].
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

  /** The Expenses tab's rows, in sheet order. */
  val expenses: MutableList<Expense> = mutableListOf()

  /** The Categories tab's rows, in sheet order. */
  val categories: MutableList<Category> = mutableListOf()

  /** What `readExpenses` returns; null means the rows of [expenses]. */
  var readExpensesResult: TabReadResult<Expense>? = null

  /** What `readCategories` returns; null means the rows of [categories]. */
  var readCategoriesResult: TabReadResult<Category>? = null

  /** What `writeExpense` returns; null means the write is applied to [expenses]. */
  var writeResult: RowWriteResult? = null

  /** Each `writeExpense` call: the Expense and the row it targeted (null for an append). */
  val writes: MutableList<Pair<Expense, Int?>> = mutableListOf()
  var readExpensesCalls = 0
  var readCategoriesCalls = 0

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

  override suspend fun readExpenses(sheetId: String): TabReadResult<Expense> =
    synchronized(this) {
      readExpensesCalls++
      readExpensesResult ?: TabReadResult.Rows(expenses.rows())
    }

  override suspend fun readCategories(sheetId: String): TabReadResult<Category> =
    synchronized(this) {
      readCategoriesCalls++
      readCategoriesResult ?: TabReadResult.Rows(categories.rows())
    }

  override suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult =
    synchronized(this) { write(expense, existingRow) }

  private fun write(expense: Expense, existingRow: Int?): RowWriteResult {
    writes += expense to existingRow
    writeResult?.let { return it }
    if (existingRow == null) {
      expenses += expense
      return RowWriteResult.Written(expenses.size + HEADER_ROWS)
    }
    val index = existingRow - 1 - HEADER_ROWS
    // An update never changes an existing Source (AD-7).
    expenses[index] = expense.copy(source = expenses[index].source)
    return RowWriteResult.Written(existingRow)
  }

  private fun <T> List<T>.rows(): List<SheetRow<T>> =
    mapIndexed { index, value -> SheetRow(index + 1 + HEADER_ROWS, value) }

  private companion object {
    const val HEADER_ROWS = 1
  }
}
