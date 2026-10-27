package com.shashi.expensetracker.data.account

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.domain.account.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
class RoomSessionRepositoryTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoomSessionRepository

  @Before
  fun setUp() {
    db =
      Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = RoomSessionRepository(db.sessionDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun observeEmitsNullWhenNothingIsStored() = runTest {
    assertNull(repository.observe().first())
  }

  @Test
  fun saveStoresAccountIdAndEmailWithNoSheetId() = runTest {
    assertTrue(repository.save(Session(accountId = "1234", email = "shylock@example.com")))

    assertEquals(Session("1234", "shylock@example.com", sheetId = null), repository.observe().first())
  }

  @Test
  fun saveReplacesTheStoredSessionInsteadOfAddingARow() = runTest {
    repository.save(Session(accountId = "1234", email = "shylock@example.com"))
    repository.save(Session(accountId = "5678", email = "portia@example.com", sheetId = "sheet-1"))

    assertEquals(Session("5678", "portia@example.com", "sheet-1"), repository.observe().first())
    db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM session").use { cursor ->
      cursor.moveToFirst()
      assertEquals(1, cursor.getInt(0))
    }
  }

  /** A DAO whose every call fails, as when the database can't be opened. */
  private class FailingSessionDao : SessionDao {
    override fun observe(): Flow<SessionEntity?> = flow { throw SQLiteException("cannot open") }

    override suspend fun upsert(session: SessionEntity) = throw SQLiteException("disk I/O error")
  }

  @Test
  fun unreadableSessionEmitsNullInsteadOfThrowing() = runTest {
    assertNull(RoomSessionRepository(FailingSessionDao()).observe().first())
  }

  @Test
  fun failedSaveReturnsFalseInsteadOfThrowing() = runTest {
    assertFalse(RoomSessionRepository(FailingSessionDao()).save(Session("1234", "shylock@example.com")))
  }
}
