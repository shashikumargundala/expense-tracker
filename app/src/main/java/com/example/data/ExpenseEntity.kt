package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long, // timestamp
    val description: String,
    val synced: Boolean = false,
    val sheetRowIndex: Int = -1 // Tracks matching row in Google Sheet
)
