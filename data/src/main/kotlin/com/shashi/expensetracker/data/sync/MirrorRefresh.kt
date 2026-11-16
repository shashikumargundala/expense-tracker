package com.shashi.expensetracker.data.sync

import androidx.room.withTransaction
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.BudgetDao
import com.shashi.expensetracker.data.budget.toDomain
import com.shashi.expensetracker.data.budget.toEntity
import com.shashi.expensetracker.data.category.CategoryDao
import com.shashi.expensetracker.data.category.toDomain
import com.shashi.expensetracker.data.category.toEntity
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.expense.toDomain
import com.shashi.expensetracker.data.expense.toEntity
import com.shashi.expensetracker.data.inbox.DismissalDao
import com.shashi.expensetracker.data.inbox.toDomain
import com.shashi.expensetracker.data.inbox.toEntity
import com.shashi.expensetracker.data.settings.SettingDao
import com.shashi.expensetracker.data.settings.toDomain
import com.shashi.expensetracker.data.settings.toEntity
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting
import javax.inject.Inject

/**
 * One successful read of all five tabs, each in sheet order, as the mirror should hold it.
 * [invalidExpenseIds] are the IDs of invalid Expenses rows (AD-7): still in the Sheet, so their
 * mirror rows are kept as they are.
 */
data class SheetContents(
  val expenses: List<Expense> = emptyList(),
  val invalidExpenseIds: Set<String> = emptySet(),
  val categories: List<Category> = emptyList(),
  val budgets: List<Budget> = emptyList(),
  val dismissals: List<Dismissal> = emptyList(),
  val settings: List<Setting> = emptyList(),
)

/**
 * Applies a successful read of the Sheet to the mirror as one atomic diff (AD-18). Inside one Room
 * transaction it reads the outbox, then, for each of the five tables, upserts only rows that changed
 * or are new and deletes only rows missing from the Sheet. A row with a pending outbox entry is left
 * exactly as it is, so a change saved during the run is never overwritten or erased. No table is
 * cleared and refilled.
 *
 * So every row without a pending entry holds its last-pulled values, except that an Expense's or
 * Budget's Category is spelled as its Category row (AD-9), which may differ from the Sheet cell in
 * case or spacing. That row is the base a later edit copies into its outbox entry (AD-4), so a base
 * comparison must compare Category by `CategoryKey`. When a key appears twice in a tab, the first
 * row in sheet order wins.
 */
class MirrorRefresh
@Inject
constructor(
  private val db: AppDatabase,
  private val expenses: ExpenseDao,
  private val categories: CategoryDao,
  private val budgets: BudgetDao,
  private val dismissals: DismissalDao,
  private val settings: SettingDao,
  private val outbox: OutboxDao,
) {

  /** Throws on a Room error, leaving every table as it was. */
  suspend operator fun invoke(sheet: SheetContents) {
    db.withTransaction {
      diff(
        sheet = sheet.expenses,
        pending = outbox.rowKeys(OutboxType.expenseTypes).toSet(),
        key = { it.id },
        current = expenses.getAll().map { it.toDomain() },
        upsert = { rows -> expenses.upsertAll(rows.map { it.toEntity() }) },
        delete = { rows -> rows.map { it.id }.chunked(MAX_ARGS).forEach { expenses.deleteByIds(it) } },
        keep = sheet.invalidExpenseIds,
      )
      diff(
        sheet = sheet.categories,
        // Category names compare trimmed and case-folded (AD-9).
        pending = outbox.rowKeys(OutboxType.categoryTypes).mapTo(HashSet(), CategoryKey::of),
        key = { CategoryKey.of(it.name) },
        current = categories.getAll().map { it.toDomain() },
        upsert = { rows -> categories.upsertAll(rows.map { it.toEntity() }) },
        delete = { rows -> rows.map { it.name }.chunked(MAX_ARGS).forEach { categories.deleteByNames(it) } },
      )
      diff(
        sheet = sheet.budgets,
        pending = outbox.rowKeys(OutboxType.budgetTypes).mapTo(HashSet(), ::budgetKey),
        key = { budgetKey("${it.month}$BUDGET_KEY_SEPARATOR${it.category}") },
        current = budgets.getAll().map { it.toDomain() },
        upsert = { rows -> budgets.upsert(rows.map { it.toEntity() }) },
        delete = { rows -> budgets.delete(rows.map { it.toEntity() }) },
      )
      diff(
        sheet = sheet.dismissals,
        pending = outbox.rowKeys(OutboxType.dismissalTypes).toSet(),
        key = { it.smsId },
        current = dismissals.getAll().map { it.toDomain() },
        upsert = { rows -> dismissals.upsert(rows.map { it.toEntity() }) },
        delete = { rows -> dismissals.delete(rows.map { it.toEntity() }) },
      )
      diff(
        sheet = sheet.settings,
        pending = outbox.rowKeys(OutboxType.settingTypes).toSet(),
        key = { it.key },
        current = settings.getAll().map { it.toDomain() },
        upsert = { rows -> settings.upsert(rows.map { it.toEntity() }) },
        delete = { rows -> settings.delete(rows.map { it.toEntity() }) },
      )
    }
  }

  /**
   * Upserts the rows of [sheet] that are new or differ from [current], and deletes the rows of
   * [current] whose key is no longer in [sheet], skipping every key in [pending] both ways and every
   * key in [keep] (rows still in the Sheet but unreadable) when deleting.
   */
  private suspend fun <T, K> diff(
    sheet: List<T>,
    pending: Set<K>,
    key: (T) -> K,
    current: List<T>,
    upsert: suspend (List<T>) -> Unit,
    delete: suspend (List<T>) -> Unit,
    keep: Set<K> = emptySet(),
  ) {
    val pulled = sheet.distinctBy(key)
    val byKey = current.associateBy(key)
    val changed = pulled.filter { key(it) !in pending && byKey[key(it)] != it }
    if (changed.isNotEmpty()) upsert(changed)
    val inSheet = pulled.mapTo(HashSet(), key)
    val missing = byKey.filterKeys { it !in inSheet && it !in pending && it !in keep }.values.toList()
    if (missing.isNotEmpty()) delete(missing)
  }

  private companion object {
    /** Stays under SQLite's bound-argument limit. */
    const val MAX_ARGS = 500

    /** A Budget's `row_key` is `yyyy-MM|Category` (AD-3, AD-7). */
    const val BUDGET_KEY_SEPARATOR = '|'

    /** A Budget `row_key` with its Category case-folded (AD-9). */
    fun budgetKey(rowKey: String): String {
      val month = rowKey.substringBefore(BUDGET_KEY_SEPARATOR)
      return "$month$BUDGET_KEY_SEPARATOR${CategoryKey.of(rowKey.substringAfter(BUDGET_KEY_SEPARATOR, ""))}"
    }
  }
}
