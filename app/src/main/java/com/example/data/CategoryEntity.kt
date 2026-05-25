package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val budgetLimit: Double, // 0.0 means no limit
    val colorHex: String,
    val iconName: String
)
