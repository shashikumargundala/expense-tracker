package com.shashi.expensetracker.data.category

import com.shashi.expensetracker.domain.category.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryModule {
  @Binds abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository
}
