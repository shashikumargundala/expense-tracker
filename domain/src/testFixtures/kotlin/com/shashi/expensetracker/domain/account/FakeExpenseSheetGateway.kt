package com.shashi.expensetracker.domain.account

import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting

/**
 * In-memory [ExpenseSheetGateway] shared by `:domain`, `:data` and `:app` tests. Set [findResult],
 * [createResult] and [checkResult] to script outcomes; [findCalls], [createCalls] and [checkCalls]
 * count the calls, and [checkedIds] records each checked Sheet ID.
 *
 * The five tabs are [expenseRows], [categories], [budgets], [dismissals] and [settings], in sheet
 * order, under a header in row 1 (so the first row is row 2). [expenseRows] holds raw Expenses rows
 * (a blank ID, a blank Source or an invalid row); [expenses] is a view of its valid rows, where a
 * plain add appends a valid row with its ID and a filled Source. By default the layout is already
 * complete, reads return the tabs and writes succeed; set [repairResult], a `read*Result`,
 * [writeResult], [appendCategoriesResult], [repairWriteResult], [deleteResult] or
 * [appendDismissalsResult] to script a failure instead. Every Expense write is recorded in [writes],
 * every layout repair counted in [repairCalls], every ID repair's fixes in [repairWrites], every
 * delete's rows in [deletes], and every appended Category or Dismissal kept in [appendedCategories]
 * or [appendedDismissals] as well as added to its tab. Deletes shift the rows below them.
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

  /** The Expenses tab's raw rows, in sheet order. */
  val expenseRows: MutableList<ExpenseRowRead> = mutableListOf()

  /** The valid rows of [expenseRows], in sheet order; changes write through to [expenseRows]. */
  val expenses: MutableList<Expense> =
    object : AbstractMutableList<Expense>() {
      private fun valid(): List<Int> = expenseRows.indices.filter { expenseRows[it].expense != null }

      override val size: Int
        get() = valid().size

      override fun get(index: Int): Expense = checkNotNull(expenseRows[valid()[index]].expense)

      override fun set(index: Int, element: Expense): Expense =
        checkNotNull(expenseRows.set(valid()[index], ExpenseRowRead(element.id, element)).expense)

      override fun add(index: Int, element: Expense) {
        val at = if (index == size) expenseRows.size else valid()[index]
        expenseRows.add(at, ExpenseRowRead(element.id, element))
      }

      override fun removeAt(index: Int): Expense = checkNotNull(expenseRows.removeAt(valid()[index]).expense)
    }

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
  var readExpensesResult: TabReadResult<ExpenseRowRead>? = null
  var readCategoriesResult: TabReadResult<Category>? = null
  var readBudgetsResult: TabReadResult<Budget>? = null
  var readDismissalsResult: TabReadResult<Dismissal>? = null
  var readSettingsResult: TabReadResult<Setting>? = null

  /** What `writeExpense` returns; null means the write is applied to [expenses]. */
  var writeResult: RowWriteResult? = null

  /** What `appendCategories` returns; null means the Categories are appended to [categories]. */
  var appendCategoriesResult: RowWriteResult? = null

  /** What `writeExpenseRepairs` returns; null means the fixes are applied to [expenseRows]. */
  var repairWriteResult: RowWriteResult? = null

  /** What `deleteExpenseRows` returns; null means the rows are removed from [expenseRows]. */
  var deleteResult: RowWriteResult? = null

  /** What `appendDismissals` returns; null means the Dismissals are appended to [dismissals]. */
  var appendDismissalsResult: RowWriteResult? = null

  /** Each `writeExpenseRepairs` call's fixes, in order. */
  val repairWrites: MutableList<List<ExpenseRowFix>> = mutableListOf()

  /** Each `deleteExpenseRows` call's rows, as passed. */
  val deletes: MutableList<List<Int>> = mutableListOf()

  /** Every Dismissal passed to a successful `appendDismissals`, in order. */
  val appendedDismissals: MutableList<Dismissal> = mutableListOf()

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

  override suspend fun readExpenses(sheetId: String): TabReadResult<ExpenseRowRead> =
    synchronized(this) {
      readExpensesCalls++
      readExpensesResult ?: TabReadResult.Rows(expenseRows.rows(), skipped = expenseRows.count { it.expense == null })
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

  override suspend fun writeExpenseRepairs(sheetId: String, fixes: List<ExpenseRowFix>): RowWriteResult =
    synchronized(this) {
      repairWrites += fixes
      repairWriteResult?.let { return it }
      fixes.forEach { fix ->
        val index = fix.row - 1 - HEADER_ROWS
        val old = expenseRows[index]
        val id = fix.id ?: old.id
        val expense =
          old.expense?.let { e ->
            e.copy(
              id = id.orEmpty(),
              source = if (fix.sourceUser) ExpenseSource.User else e.source,
              smsId = if (fix.clearSmsId) null else e.smsId,
            )
          }
        expenseRows[index] = ExpenseRowRead(id, expense, sourceBlank = old.sourceBlank && !fix.sourceUser)
      }
      RowWriteResult.Written(null)
    }

  override suspend fun deleteExpenseRows(sheetId: String, rows: List<Int>): RowWriteResult =
    synchronized(this) {
      deletes += rows
      deleteResult?.let { return it }
      rows.sortedDescending().forEach { expenseRows.removeAt(it - 1 - HEADER_ROWS) }
      RowWriteResult.Written(null)
    }

  override suspend fun appendDismissals(sheetId: String, dismissals: List<Dismissal>): RowWriteResult =
    synchronized(this) {
      appendDismissalsResult?.let { return it }
      val first = this.dismissals.size + 1 + HEADER_ROWS
      this.dismissals += dismissals
      appendedDismissals += dismissals
      RowWriteResult.Written(first)
    }

  private fun write(expense: Expense, existingRow: Int?): RowWriteResult {
    writes += expense to existingRow
    writeResult?.let { return it }
    if (existingRow == null) {
      expenseRows += ExpenseRowRead(expense.id, expense)
      return RowWriteResult.Written(expenseRows.size + HEADER_ROWS)
    }
    val index = existingRow - 1 - HEADER_ROWS
    val old = expenseRows[index]
    // An update never changes an existing Source (AD-7).
    val source = old.expense?.source ?: ExpenseSource.User
    expenseRows[index] = ExpenseRowRead(expense.id, expense.copy(source = source), old.sourceBlank)
    return RowWriteResult.Written(existingRow)
  }

  private fun <T> List<T>.rows(): List<SheetRow<T>> =
    mapIndexed { index, value -> SheetRow(index + 1 + HEADER_ROWS, value) }

  private companion object {
    const val HEADER_ROWS = 1
  }
}
