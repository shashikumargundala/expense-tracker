package com.shashi.expensetracker.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.expense.RoomExpenseRepository
import com.shashi.expensetracker.data.inMemoryDatabase
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.data.testClock
import com.shashi.expensetracker.domain.account.ExpenseSheetGateway
import com.shashi.expensetracker.domain.account.FakeExpenseSheetGateway
import com.shashi.expensetracker.domain.account.Session
import com.shashi.expensetracker.domain.account.SessionRepository
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.sync.SyncState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncCoordinatorTest {

  private class Sessions(initial: Session?) : SessionRepository {
    val session = MutableStateFlow(initial)

    override fun observe(): Flow<Session?> = session

    override suspend fun save(session: Session): Boolean = true
  }

  /** Delegates to [sheet] but holds every Expenses read until [gate] completes. */
  private class GatedGateway(val sheet: FakeExpenseSheetGateway) : ExpenseSheetGateway by sheet {
    val gate = CompletableDeferred<Unit>()
    val entered = CompletableDeferred<Unit>()

    override suspend fun readExpenses(sheetId: String): TabReadResult<Expense> {
      entered.complete(Unit)
      gate.await()
      return sheet.readExpenses(sheetId)
    }
  }

  private lateinit var db: AppDatabase
  private val sheet = FakeExpenseSheetGateway()
  private val sessions = Sessions(Session("acct", "tester@example.com", sheetId = "sheet-1"))

  @Before fun setUp() { db = inMemoryDatabase() }

  @After fun tearDown() = db.close()

  private fun engine(gateway: ExpenseSheetGateway) =
    SyncEngine(gateway, db.expenseDao(), db.outboxDao(), MirrorRefresh(db, db.expenseDao(), db.categoryDao(), db.outboxDao()))

  @Test
  fun `a run syncs the stored session's Sheet`() = runTest {
    RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock).add(sampleExpense())
    val coordinator = SyncCoordinator(engine(sheet), sessions, db.outboxDao(), backgroundScope)

    assertEquals(SyncResult.Synced(pushed = 1, skipped = 0), coordinator.runOnce())
    assertEquals(1, sheet.expenses.size)
  }

  @Test
  fun `without a Sheet ID nothing runs`() = runTest {
    sessions.session.value = Session("acct", "tester@example.com", sheetId = null)
    val coordinator = SyncCoordinator(engine(sheet), sessions, db.outboxDao(), backgroundScope)

    assertNull(coordinator.runOnce())
    sessions.session.value = null
    assertNull(coordinator.runOnce())
    assertEquals(0, sheet.readExpensesCalls)
  }

  @Test
  fun `a request while a run is in progress is dropped`() = runTest {
    val gated = GatedGateway(sheet)
    val coordinator = SyncCoordinator(engine(gated), sessions, db.outboxDao(), backgroundScope)

    val first = async { coordinator.runOnce() }
    gated.entered.await()
    assertEquals(SyncState(running = true, pendingCount = 0), coordinator.observeState().first())

    assertNull(coordinator.runOnce())

    gated.gate.complete(Unit)
    assertTrue(first.await() is SyncResult.Synced)
    assertEquals(1, sheet.readExpensesCalls)
    assertEquals(SyncState(running = false, pendingCount = 0), coordinator.observeState().first())
  }

  @Test
  fun `state counts pending entries`() = runTest {
    val coordinator = SyncCoordinator(engine(sheet), sessions, db.outboxDao(), backgroundScope)
    RoomExpenseRepository(db, db.expenseDao(), db.outboxDao(), testClock).add(sampleExpense())

    assertEquals(SyncState(running = false, pendingCount = 1), coordinator.observeState().first())
  }
}
