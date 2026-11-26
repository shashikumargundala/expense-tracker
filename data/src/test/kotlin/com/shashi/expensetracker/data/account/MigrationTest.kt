package com.shashi.expensetracker.data.account

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shashi.expensetracker.data.sync.OutboxEntity
import com.shashi.expensetracker.data.sync.OutboxType
import com.shashi.expensetracker.domain.account.Session
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrades: a v1 database with a session migrates to v2 and keeps it; a v2 database with outbox
 * rows migrates to v3 and keeps them, with a null `payload`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

  @get:Rule
  val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

  @Test
  fun version1SessionSurvivesTheMigrationToVersion2() = runTest {
    helper.createDatabase(DB, 1).apply {
      execSQL(
        "INSERT INTO session (id, account_id, email, sheet_id) VALUES (0, 'acct-1', 'shylock@example.com', 'sheet-1')"
      )
      close()
    }

    // Validates the migrated schema against the exported 2.json.
    helper.runMigrationsAndValidate(DB, 2, true).close()

    val db =
      Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java, DB)
        .allowMainThreadQueries()
        .build()
    try {
      assertEquals(
        Session(accountId = "acct-1", email = "shylock@example.com", sheetId = "sheet-1"),
        RoomSessionRepository(db.sessionDao()).observe().first(),
      )
      listOf("expenses", "categories", "budgets", "dismissed", "settings", "outbox").forEach { table ->
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
          cursor.moveToFirst()
          assertEquals(table, 0, cursor.getInt(0))
        }
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun version2OutboxRowsSurviveTheMigrationToVersion3WithANullPayload() = runTest {
    helper.createDatabase(DB, 2).apply {
      execSQL(
        "INSERT INTO session (id, account_id, email, sheet_id) VALUES (0, 'acct-1', 'shylock@example.com', 'sheet-1')"
      )
      execSQL(
        "INSERT INTO outbox (id, type, row_key, base, created_at) VALUES " +
          "(1, 'UpsertExpense', 'a', NULL, '2026-11-13T09:30'), (2, 'DeleteExpense', 'b', '{}', '2026-11-13T09:31')"
      )
      close()
    }

    // Validates the migrated schema against the exported 3.json.
    helper.runMigrationsAndValidate(DB, 3, true).close()

    val db =
      Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java, DB)
        .allowMainThreadQueries()
        .build()
    try {
      assertEquals(
        Session(accountId = "acct-1", email = "shylock@example.com", sheetId = "sheet-1"),
        RoomSessionRepository(db.sessionDao()).observe().first(),
      )
      assertEquals(
        listOf(
          OutboxEntity(1, OutboxType.UpsertExpense, "a", null, LocalDateTime.of(2026, 11, 13, 9, 30), payload = null),
          OutboxEntity(2, OutboxType.DeleteExpense, "b", "{}", LocalDateTime.of(2026, 11, 13, 9, 31), payload = null),
        ),
        db.outboxDao().getAll(),
      )
    } finally {
      db.close()
    }
  }

  private companion object {
    const val DB = "migration-test.db"
  }
}
