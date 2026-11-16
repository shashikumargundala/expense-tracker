package com.shashi.expensetracker.data.expense

import com.shashi.expensetracker.domain.expense.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Binds the Expense mirror and provides the [Clock] "now" comes from (AD-19). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseModule {

  @Binds abstract fun bindExpenseRepository(impl: RoomExpenseRepository): ExpenseRepository

  companion object {
    @Provides @Singleton fun provideClock(): Clock = Clock.systemDefaultZone()
  }
}
