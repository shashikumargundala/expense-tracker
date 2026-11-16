package com.shashi.expensetracker.data.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A mirrored Settings row, keyed by [key] (AD-7, AD-20). */
@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)
