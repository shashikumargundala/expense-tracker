package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting

/**
 * In-memory [ExpenseSheetGateway] shared by `:domain`, `:data` and `:app` tests. Set [findResult],
 * [createResult] and [checkResult] to script outcomes; [findCalls], [createCalls] and [checkCalls]
 * count the calls, and [checkedIds] records each checked Sheet ID.
 *
 * The five tabs are [expenses], [categories], [budgets], [dismissals] and [settings], in sheet
 * order, under a header in row 1 (so the first row is row 2). By default the layout is already
 * complete, reads return the tabs and writes succeed; set [repairResult], a `read*Result`,
 * [writeResult] or [appendCategoriesResult] to script a failure instead. Every Expense write is
 * recorded in [writes], every repair counted in [repairCalls], and every appended Category kept in
 * [appendedCategories] as well as added to [categories].
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

  /** The Budgets tab's rows, in sheet order. */
  val budgets: MutableList<Budget> = mutableListOf()

  /** The Dismissed tab's rows, in sheet order. */
  val dismissals: MutableList<Dismissal> = mutableListOf()

  /** The Settings tab's rows, in sheet order. */
  val settings: MutableList<Setting> = mutableListOf()

  /** What `repairLayout` returns; null means the layout is complete. */
  var repairResult: RowWriteResult? = null
  var repairCalls = 0

  /** What each read returns; null means the rows of its tab. */
  var readExpensesResult: TabReadResult<Expense>? = null
  var readCategoriesResult: TabReadResult<Category>? = null
  var readBudgetsResult: TabReadResult<Budget>? = null
  var readDismissalsResult: TabReadResult<Dismissal>? = null
  var readSettingsResult: TabReadResult<Setting>? = null

  /** What `writeExpense` returns; null means the write is applied to [expenses]. */
  var writeResult: RowWriteResult? = null

  /** What `appendCategories` returns; null means the Categories are appended to [categories]. */
  var appendCategoriesResult: RowWriteResult? = null

  /** Each `writeExpense` call: the Expense and the row it targeted (null for an append). */
  val writes: MutableList<Pair<Expense, Int?>> = mutableListOf()

  /** Every Category passed to a successful `appendCategories`, in order. */
  val appendedCategories: MutableList<Category> = mutableListOf()
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

  override suspend fun repairLayout(sheetId: String): RowWriteResult =
    synchronized(this) {
      repairCalls++
      repairResult ?: RowWriteResult.Written(null)
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

  override suspend fun readBudgets(sheetId: String): TabReadResult<Budget> =
    synchronized(this) { readBudgetsResult ?: TabReadResult.Rows(budgets.rows()) }

  override suspend fun readDismissals(sheetId: String): TabReadResult<Dismissal> =
    synchronized(this) { readDismissalsResult ?: TabReadResult.Rows(dismissals.rows()) }

  override suspend fun readSettings(sheetId: String): TabReadResult<Setting> =
    synchronized(this) { readSettingsResult ?: TabReadResult.Rows(settings.rows()) }

  override suspend fun writeExpense(sheetId: String, expense: Expense, existingRow: Int?): RowWriteResult =
    synchronized(this) { write(expense, existingRow) }

  override suspend fun appendCategories(sheetId: String, categories: List<Category>): RowWriteResult =
    synchronized(this) {
      appendCategoriesResult?.let { return it }
      val first = this.categories.size + 1 + HEADER_ROWS
      this.categories += categories
      appendedCategories += categories
      RowWriteResult.Written(first)
    }

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
