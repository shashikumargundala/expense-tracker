package com.shashi.expensetracker.domain.category

import kotlinx.coroutines.flow.Flow

/** Categories in the local mirror. Implemented in `:data`, which maps storage errors. */
interface CategoryRepository {
  /** Emits every Category by name, and again on every change. An unreadable mirror emits empty. */
  fun observe(): Flow<List<Category>>
}
