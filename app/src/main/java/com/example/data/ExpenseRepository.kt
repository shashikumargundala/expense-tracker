package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    private val sharedPrefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    var spreadsheetId: String?
        get() = sharedPrefs.getString("spreadsheet_id", null)
        set(value) = sharedPrefs.edit().putString("spreadsheet_id", value).apply()

    var oauthToken: String?
        get() = sharedPrefs.getString("oauth_token", null)
        set(value) = sharedPrefs.edit().putString("oauth_token", value).apply()

    var userName: String?
        get() = sharedPrefs.getString("user_name", "Local Session Tracker")
        set(value) = sharedPrefs.edit().putString("user_name", value).apply()

    var userEmail: String?
        get() = sharedPrefs.getString("user_email", "offline@local.app")
        set(value) = sharedPrefs.edit().putString("user_email", value).apply()

    var googleClientId: String?
        get() = sharedPrefs.getString("google_client_id", "703403170183-googleclientid.apps.googleusercontent.com")
        set(value) = sharedPrefs.edit().putString("google_client_id", value).apply()

    var googleClientSecret: String?
        get() = sharedPrefs.getString("google_client_secret", "")
        set(value) = sharedPrefs.edit().putString("google_client_secret", value).apply()

    var refreshTokenSaved: String?
        get() = sharedPrefs.getString("refresh_token_saved", null)
        set(value) = sharedPrefs.edit().putString("refresh_token_saved", value).apply()

    // Default Labels: Personal, Food, Entertainment, Savings, Travel, Householdings
    val defaultCategories = listOf(
        CategoryEntity("Personal", 500.0, "#9C27B0", "Person"),
        CategoryEntity("Food", 400.0, "#4CAF50", "Lunch"),
        CategoryEntity("Entertainment", 200.0, "#FF9800", "Movie"),
        CategoryEntity("Savings", 1000.0, "#009688", "Wallet"),
        CategoryEntity("Travel", 600.0, "#2196F3", "Flight"),
        CategoryEntity("Household", 800.0, "#E91E63", "Home")
    )

    // Flow for UI
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun checkAndBootstrapCategories() {
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getAllCategories()
            if (existing.isEmpty()) {
                categoryDao.insertCategories(defaultCategories)
                Log.d("ExpenseRepository", "Bootstrapped def categories")
            }
        }
    }

    suspend fun saveExpense(expense: ExpenseEntity): Long {
        return withContext(Dispatchers.IO) {
            val localId = expenseDao.insertExpense(expense)
            val updatedExpense = expense.copy(id = localId.toInt())
            // Attempt back-sync if sheets config is active
            if (spreadsheetId != null && oauthToken != null) {
                try {
                    syncSingleToSheets(updatedExpense)
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "Sheets sync failed, saved locally", e)
                }
            }
            localId
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        withContext(Dispatchers.IO) {
            expenseDao.deleteExpense(expense)
            // In a real sandbox or device, deleting removes locally.
        }
    }

    suspend fun deleteExpenseById(id: Int) {
        withContext(Dispatchers.IO) {
            expenseDao.deleteById(id)
        }
    }

    suspend fun saveCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.insertCategory(category)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    }

    suspend fun deleteCategoryByName(name: String) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteByName(name)
        }
    }

    // CREATE NEW GOOGLE SPREADSHEET
    suspend fun createGoogleSheet(): String {
        return withContext(Dispatchers.IO) {
            val token = oauthToken ?: throw IllegalStateException("Access token missing, please sign-in first.")
            val service = RetrofitClient.sheetsService
            val request = CreateSpreadsheetRequest(
                properties = SpreadsheetProperties("Expense Tracker Sheets Book")
            )
            val authHeader = "Bearer $token"
            val response = service.createSpreadsheet(authHeader, request)
            val sheetId = response.spreadsheetId
            spreadsheetId = sheetId

            // Format Header Row
            val headers = listOf("Local ID", "Timestamp", "Amount ($)", "Category", "Description")
            val valRequest = ValueRangeRequest(
                range = "Sheet1!A1:E1",
                values = listOf(headers)
            )
            service.appendSpreadsheetValues(authHeader, sheetId, "Sheet1!A1", body = valRequest)

            // Send any existing offline data as well
            val unsynced = expenseDao.getUnsyncedExpenses()
            if (unsynced.isNotEmpty()) {
                val rows = unsynced.map {
                    listOf(
                        it.id.toString(),
                        formatTimestamp(it.date),
                        it.amount.toString(),
                        it.category,
                        it.description
                    )
                }
                val bulkRequest = ValueRangeRequest(
                    range = "Sheet1!A2",
                    values = rows
                )
                service.appendSpreadsheetValues(authHeader, sheetId, "Sheet1!A2", body = bulkRequest)
                // Mark synced
                val syncedList = unsynced.map { it.copy(synced = true) }
                expenseDao.insertExpenses(syncedList)
            }

            sheetId
        }
    }

    // SYNC LOCAL UNSYNCED TO SHEETS
    suspend fun syncLocalToSheets() {
        withContext(Dispatchers.IO) {
            val token = oauthToken ?: return@withContext
            val sheetId = spreadsheetId ?: return@withContext
            val unsynced = expenseDao.getUnsyncedExpenses()
            if (unsynced.isEmpty()) return@withContext

            val rows = unsynced.map {
                listOf(
                    it.id.toString(),
                    formatTimestamp(it.date),
                    it.amount.toString(),
                    it.category,
                    it.description
                )
            }
            val authHeader = "Bearer $token"
            val body = ValueRangeRequest(
                range = "Sheet1!A2",
                values = rows
            )
            try {
                RetrofitClient.sheetsService.appendSpreadsheetValues(authHeader, sheetId, "Sheet1!A2", body = body)
                // update local db records as synced
                val syncedList = unsynced.map { it.copy(synced = true) }
                expenseDao.insertExpenses(syncedList)
                Log.d("ExpenseRepository", "Successfully synced local items to spreadsheet!")
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Bulk sync error", e)
            }
        }
    }

    private suspend fun syncSingleToSheets(expense: ExpenseEntity) {
        val token = oauthToken ?: return
        val sheetId = spreadsheetId ?: return
        val authHeader = "Bearer $token"
        val row = listOf(
            expense.id.toString(),
            formatTimestamp(expense.date),
            expense.amount.toString(),
            expense.category,
            expense.description
        )
        val body = ValueRangeRequest(
            range = "Sheet1!A2",
            values = listOf(row)
        )
        RetrofitClient.sheetsService.appendSpreadsheetValues(authHeader, sheetId, "Sheet1!A2", body = body)
        expenseDao.updateExpense(expense.copy(synced = true))
    }

    // PULL DATA FROM SHEETS TO DISCOVER & RESOLVE SYNC
    suspend fun pullDataFromSheets(): Int {
        return withContext(Dispatchers.IO) {
            val token = oauthToken ?: return@withContext 0
            val sheetId = spreadsheetId ?: return@withContext 0
            val authHeader = "Bearer $token"
            try {
                val response = RetrofitClient.sheetsService.getSpreadsheetValues(authHeader, sheetId, "Sheet1!A2:E1000")
                val rows = response.values ?: return@withContext 0
                val activeCategories = categoryDao.getAllCategories().map { it.name }.toSet()

                val downloadedList = ArrayList<ExpenseEntity>()
                val categoriesToInsert = ArrayList<CategoryEntity>()

                for (row in rows) {
                    if (row.size < 4) continue // localId, timestamp, amount, category, description
                    val localIdStr = row.getOrNull(0) ?: ""
                    val timestampStr = row.getOrNull(1) ?: ""
                    val amountStr = row.getOrNull(2) ?: "0"
                    val categoryStr = row.getOrNull(3) ?: "Other"
                    val descStr = row.getOrNull(4) ?: ""

                    val localId = localIdStr.toIntOrNull() ?: 1
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val date = parseTimestamp(timestampStr)

                    // Automatically build category if missing locally
                    if (!activeCategories.contains(categoryStr) && categoryStr.isNotBlank() && categoriesToInsert.none { it.name == categoryStr }) {
                        categoriesToInsert.add(CategoryEntity(categoryStr, 0.0, "#9E9E9E", "Category"))
                    }

                    downloadedList.add(
                        ExpenseEntity(
                            id = localId,
                            amount = amount,
                            category = categoryStr,
                            date = date,
                            description = descStr,
                            synced = true
                        )
                    )
                }

                if (categoriesToInsert.isNotEmpty()) {
                    categoryDao.insertCategories(categoriesToInsert)
                }

                if (downloadedList.isNotEmpty()) {
                    // Overwrite/insert into local db
                    expenseDao.insertExpenses(downloadedList)
                }
                downloadedList.size
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Pull spreadsheet failed", e)
                throw e
            }
        }
    }

    // UTILITIES
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun parseTimestamp(text: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(text)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(text)?.time ?: System.currentTimeMillis()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    // NATIVE GOOGLE OAUTH TOKEN OPERATIONS
    suspend fun exchangeCodeForToken(code: String, clientId: String, clientSecret: String): String {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient()
            val formBody = okhttp3.FormBody.Builder()
                .add("code", code)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", "com.example.expensetracker://oauth2callback")
                .add("grant_type", "authorization_code")
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    throw Exception("OAuth token exchange failed: ${response.code} $bodyStr")
                }
                val bodyStr = response.body?.string() ?: throw Exception("Empty token body")
                val json = org.json.JSONObject(bodyStr)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token")
                
                oauthToken = accessToken
                if (!refreshToken.isNullOrEmpty()) {
                    refreshTokenSaved = refreshToken
                }
                accessToken
            }
        }
    }

    suspend fun refreshGoogleToken(): String? {
        val rToken = refreshTokenSaved ?: return null
        val clientId = googleClientId ?: return null
        val clientSecret = googleClientSecret ?: ""
        return withContext(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val formBody = okhttp3.FormBody.Builder()
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("refresh_token", rToken)
                    .add("grant_type", "refresh_token")
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = org.json.JSONObject(bodyStr)
                    val accessToken = json.getString("access_token")
                    oauthToken = accessToken
                    accessToken
                }
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to refresh token", e)
                null
            }
        }
    }
}
