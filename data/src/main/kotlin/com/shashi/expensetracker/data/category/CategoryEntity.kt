package com.shashi.expensetracker.data.category

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shashi.expensetracker.domain.category.Category

/**
 * A mirrored Categories row. [name] is the key, compared case-insensitively (AD-9); names arrive
 * trimmed from the Sheet mapping.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
  val color: String,
  val icon: String,
)

fun CategoryEntity.toDomain(): Category = Category(name = name, color = color, icon = icon)

fun Category.toEntity(): CategoryEntity = CategoryEntity(name = name, color = color, icon = icon)
