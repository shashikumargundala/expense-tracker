package com.shashi.expensetracker.data.expense

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.sync.OutboxDao
import com.shashi.expensetracker.data.sync.OutboxEntity
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.expense.ExpenseListItem
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExpenseRepositoryTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomExpenseRepository

  @Before
  fun setUp() {
    db = inMemoryDatabase()
    repository = RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock)
  }

  @After fun tearDown() = db.close()

  @Test
  fun addStoresTheExpenseAndOneUpsertExpenseEntryWithNoBase() = runTest {
    val expense = sampleExpense()

    assertTrue(repository.add(expense))

    assertEquals(expense, db.expenseDao().get(expense.id)?.toDomain())
    val entry = db.outboxDao().getAll().single()
    assertEquals(OutboxType.UpsertExpense, entry.type)
    assertEquals(expense.id, entry.rowKey)
    assertNull(entry.base)
    assertEquals(LocalDateTime.of(2026, 11, 13, 9, 30), entry.createdAt)
  }

  @Test
  fun addedExpenseListsAsPendingUntilItsEntryLeaves() = runTest {
    val expense = sampleExpense()
    repository.add(expense)

    assertEquals(listOf(ExpenseListItem(expense, pending = true)), repository.observe().first())

    db.outboxDao().delete(db.outboxDao().getAll().single().id)

    assertEquals(listOf(ExpenseListItem(expense, pending = false)), repository.observe().first())
  }

  @Test
  fun observeListsNewestFirst() = runTest {
    val older = sampleExpense(id = "a", date = LocalDateTime.of(2026, 11, 1, 8, 0))
    val newer = sampleExpense(id = "b", date = LocalDateTime.of(2026, 11, 12, 8, 0))
    repository.add(older)
    repository.add(newer)

    assertEquals(listOf("b", "a"), repository.observe().first().map { it.expense.id })
  }

  /** An outbox whose insert fails, as when the disk is full mid-transaction. */
  private class FailingOutboxDao(private val real: OutboxDao) : OutboxDao by real {
    override suspend fun insert(entry: OutboxEntity): Long = throw IllegalStateException("disk full")
  }

  @Test
  fun aFailedOutboxWriteRollsBackTheExpense() = runTest {
    val failing = RoomExpenseRepository(db, db.expenseDao(), FailingOutboxDao(db.outboxDao()), testClock)

    assertFalse(failing.add(sampleExpense()))

    assertTrue(db.expenseDao().getAll().isEmpty())
    assertTrue(db.outboxDao().getAll().isEmpty())
  }

  private class FailingExpenseDao(private val real: ExpenseDao) : ExpenseDao by real {
    override fun observeWithPending(): Flow<List<ExpenseWithPending>> =
      kotlinx.coroutines.flow.flow { throw android.database.sqlite.SQLiteException("cannot open") }
  }

  @Test
  fun anUnreadableMirrorEmitsAnEmptyList() = runTest {
    val broken = RoomExpenseRepository(db, FailingExpenseDao(db.expenseDao()), db.outboxDao(), testClock)

    assertEquals(emptyList<ExpenseListItem>(), broken.observe().first())
  }
}
