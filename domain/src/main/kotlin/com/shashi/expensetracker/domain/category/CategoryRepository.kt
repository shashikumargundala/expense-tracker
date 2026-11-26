package com.shashi.expensetracker.domain.category

import kotlinx.coroutines.flow.Flow

/**
 * Categories in the local mirror. Implemented in `:data`, which maps storage errors. Every write
 * stores the change and its outbox entry in one transaction (AD-3); names match by [CategoryKey].
 */
interface CategoryRepository {
  /** Emits every Category by name, and again on every change. An unreadable mirror emits empty. */
  fun observe(): Flow<List<Category>>

  /**
   * Stores [category] (its name trimmed), new or replacing the Category with the same key, and
   * queues its `UpsertCategory`. Returns false, with nothing written, for a blank name or when
   * storage failed.
   */
  suspend fun upsert(category: Category): Boolean

  /**
   * Deletes the Category [name] (AD-9): the mirror's Expenses that use it move to [reassignTo], its
   * Budgets are deleted, and a `DeleteCategory` is queued, all in one transaction. Returns false,
   * with nothing written, when [name] isn't a Category, [reassignTo] isn't another existing
   * Category, or storage failed.
   */
  suspend fun delete(name: String, reassignTo: String): Boolean
}
