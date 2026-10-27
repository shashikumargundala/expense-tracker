package com.shashi.expensetracker.domain.account

/**
 * The signed-in account, stored locally without secrets (AD-20). [accountId] is the Google account's
 * stable ID (the ID token's `sub`). [sheetId] is the Expense Sheet's ID, null until it is found or
 * created.
 */
data class Session(val accountId: String, val email: String, val sheetId: String? = null)
