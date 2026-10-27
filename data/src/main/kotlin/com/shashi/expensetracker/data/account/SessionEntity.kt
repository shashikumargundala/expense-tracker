package com.shashi.expensetracker.data.account

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** The one stored session row (AD-20). No token or secret is ever stored here. */
@Entity(tableName = "session")
data class SessionEntity(
  @PrimaryKey val id: Int = SINGLE_ROW_ID,
  @ColumnInfo(name = "account_id") val accountId: String,
  val email: String,
  /** The Expense Sheet's ID; null until story 2.3 finds or creates it. */
  @ColumnInfo(name = "sheet_id") val sheetId: String?,
) {
  companion object {
    const val SINGLE_ROW_ID = 0
  }
}
