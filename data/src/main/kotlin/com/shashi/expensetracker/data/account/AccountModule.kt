package com.shashi.expensetracker.data.account

import com.shashi.expensetracker.domain.account.AccountGateway
import com.shashi.expensetracker.domain.account.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the account implementations. Tests replace this module with fakes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

  @Binds abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository

  @Binds abstract fun bindAccountGateway(impl: CredentialManagerAccountGateway): AccountGateway
}
