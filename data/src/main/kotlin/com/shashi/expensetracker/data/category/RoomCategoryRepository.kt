package com.shashi.expensetracker.data.category

import android.util.Log
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.category.CategoryRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [CategoryRepository] over the Room mirror. An unreadable mirror emits an empty list. */
class RoomCategoryRepository @Inject constructor(private val categories: CategoryDao) : CategoryRepository {

  override fun observe(): Flow<List<Category>> =
    categories
      .observe()
      .map { rows -> rows.map { it.toDomain() } }
      .distinctUntilChanged()
      .catch { e ->
        if (e is CancellationException) throw e
        Log.w(TAG, "Category read failed: ${e.javaClass.simpleName}")
        emit(emptyList())
      }

  private companion object {
    const val TAG = "CategoryRepository"
  }
}
