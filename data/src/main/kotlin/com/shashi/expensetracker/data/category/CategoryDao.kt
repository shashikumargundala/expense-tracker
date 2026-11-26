package com.shashi.expensetracker.data.category

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY name") fun observe(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories") suspend fun getAll(): List<CategoryEntity>

  /** The Category named [name], ignoring case (the column is `NOCASE`); pass it trimmed. */
  @Query("SELECT * FROM categories WHERE name = :name") suspend fun get(name: String): CategoryEntity?

  @Upsert suspend fun upsert(category: CategoryEntity)

  @Upsert suspend fun upsertAll(categories: List<CategoryEntity>)

  /** Deletes by name, ignoring case (the column is `NOCASE`). */
  @Query("DELETE FROM categories WHERE name IN (:names)") suspend fun deleteByNames(names: List<String>)
}
