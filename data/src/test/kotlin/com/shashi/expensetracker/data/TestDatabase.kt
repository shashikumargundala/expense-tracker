package com.shashi.expensetracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shashi.expensetracker.data.account.AppDatabase
import com.shashi.expensetracker.data.sync.MirrorRefresh
import com.shashi.expensetracker.domain.expense.Expense
import com.shashi.expensetracker.domain.expense.ExpenseSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** A fresh in-memory [AppDatabase] for Robolectric tests. */
fun inMemoryDatabase(): AppDatabase =
  Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
    .allowMainThreadQueries()
    .build()

/** A [MirrorRefresh] over every table of this database. */
fun AppDatabase.mirrorRefresh(): MirrorRefresh =
  MirrorRefresh(this, expenseDao(), categoryDao(), budgetDao(), dismissalDao(), settingDao(), outboxDao())

/** A fixed clock at 2026-11-13 09:30 UTC. */
val testClock: Clock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneOffset.UTC)

/** An app-created Expense with [id]; other fields have sample values. */
fun sampleExpense(
  id: String = "0f8fad5b-d9cb-469f-a165-70867728950e",
  amountPaise: Long = 12_000,
  date: LocalDateTime = LocalDateTime.of(2026, 11, 13, 9, 30),
) =
  Expense(
    id = id,
    date = date,
    amountPaise = amountPaise,
    category = "Food",
    description = "Chai",
    source = ExpenseSource.App,
  )
