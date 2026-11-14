package com.shashi.expensetracker.domain.category

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Emits the mirror's Categories, for example for the Expense form's picker. Never throws. */
class ObserveCategoriesUseCase @Inject constructor(private val categories: CategoryRepository) {
  operator fun invoke(): Flow<List<Category>> = categories.observe()
}
