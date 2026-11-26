package com.shashi.expensetracker.data.category

import android.util.Log
import androidx.room.withTransaction
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.budget.BudgetDao
import com.shashi.expensetracker.data.expense.ExpenseDao
import com.shashi.expensetracker.data.sync.CategoryBase
import com.shashi.expensetracker.data.sync.OutboxDao
import com.shashi.expensetracker.data.sync.OutboxEntity
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryKey
import com.shashi.expensetracker.domain.category.CategoryRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [CategoryRepository] over the Room mirror. [upsert] and [delete] each write the mirror and the
 * outbox entry in one transaction (AD-3), with the entry's base taken from the mirror row before
 * the change (AD-4). Room errors are caught: an unreadable mirror emits an empty list and a failed
 * write returns false. Only the exception class is logged.
 */
class RoomCategoryRepository
@Inject
constructor(
  private val db: AppDatabase,
  private val categories: CategoryDao,
  private val expenses: ExpenseDao,
  private val budgets: BudgetDao,
  private val outbox: OutboxDao,
  private val clock: Clock,
) : CategoryRepository {

  override fun observe(): Flow<List<Category>> =
    categories
      .observe()
      .map { rows -> rows.map { it.toDomain() } }
      .distinctUntilChanged()
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w(TAG, "Category read failed: ${e.javaClass.simpleName}")
        emit(emptyList())
      }

  override suspend fun upsert(category: Category): Boolean {
    val name = category.name.trim()
    if (name.isEmpty()) return false
    return write("upsert") {
      val before = categories.get(name)?.toDomain()
      val stored = category.copy(name = name)
      categories.upsert(stored.toEntity())
      outbox.insert(
        OutboxEntity(
          type = OutboxType.UpsertCategory,
          rowKey = name,
          base = before?.let(CategoryBase::encode),
          createdAt = LocalDateTime.now(clock),
        )
      )
      true
    }
  }

  override suspend fun delete(name: String, reassignTo: String): Boolean {
    if (CategoryKey.of(name) == CategoryKey.of(reassignTo)) return false
    return write("delete") {
      val deleted = categories.get(name.trim())?.toDomain() ?: return@write false
      val target = categories.get(reassignTo.trim())?.toDomain() ?: return@write false
      expenses.reassignCategory(from = deleted.name.trim(), to = target.name)
      budgets.deleteByCategory(deleted.name.trim())
      categories.deleteByNames(listOf(deleted.name))
      outbox.insert(
        OutboxEntity(
          type = OutboxType.DeleteCategory,
          rowKey = deleted.name,
          base = CategoryBase.encode(deleted),
          createdAt = LocalDateTime.now(clock),
          payload = target.name,
        )
      )
      true
    }
  }

  /** Runs [block] in one transaction; false when it returns false (nothing was written) or Room fails. */
  private suspend fun write(action: String, block: suspend () -> Boolean): Boolean =
    try {
      db.withTransaction { block() }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Category $action failed: ${e.javaClass.simpleName}")
      false
    }

  private companion object {
    const val TAG = "CategoryRepository"
  }
}
