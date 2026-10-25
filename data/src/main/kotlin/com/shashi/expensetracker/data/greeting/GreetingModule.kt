package com.shashi.expensetracker.data.greeting

import com.shashi.expensetracker.domain.greeting.GreetingGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GreetingModule {
  @Binds abstract fun bindGreetingGateway(impl: StubGreetingGateway): GreetingGateway
}
