package com.shashi.expensetracker.data.expense

import android.util.Log
import androidx.room.withTransaction
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.sync.OutboxDao
import com.shashi.expensetracker.data.sync.OutboxEntity
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseListItem
import com.shashi.expensetracker.domain.expense.ExpenseRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [ExpenseRepository] over the Room mirror. [add] writes the Expense and its `UpsertExpense` outbox
 * entry in one transaction (AD-3). Room errors are caught: an unreadable mirror emits an empty list
 * and a failed add returns false. Only the exception class is logged, never an amount or text.
 */
class RoomExpenseRepository
@Inject
constructor(
  private val db: AppDatabase,
  private val expenses: ExpenseDao,
  private val outbox: OutboxDao,
  private val clock: Clock,
) : ExpenseRepository {

  override fun observe(): Flow<List<ExpenseListItem>> =
    expenses
      .observeWithPending()
      .map { rows -> rows.map { ExpenseListItem(expense = it.expense.toDomain(), pending = it.pending) } }
      .distinctUntilChanged()
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w(TAG, "Expense read failed: ${e.javaClass.simpleName}")
        emit(emptyList())
      }

  override suspend fun add(expense: Expense): Boolean =
    try {
      db.withTransaction {
        expenses.upsert(expense.toEntity())
        // Entry 1 creates only: the row is absent from the Sheet as far as this phone knows.
        outbox.insert(
          OutboxEntity(
            type = OutboxType.UpsertExpense,
            rowKey = expense.id,
            base = null,
            createdAt = LocalDateTime.now(clock),
          )
        )
      }
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Expense add failed: ${e.javaClass.simpleName}")
      false
    }

  private companion object {
    const val TAG = "ExpenseRepository"
  }
}
