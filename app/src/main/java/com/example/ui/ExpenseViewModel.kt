package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // Main States
    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI filters
    var selectedCategoryFilter by mutableStateOf<String?>(null)
    var searchKeyword by mutableStateOf("")

    // Sync state
    var isSyncing by mutableStateOf(false)
    var syncMessage by mutableStateOf<String?>(null)

    // OAuth simulated login & user state
    var isLoggedIn by mutableStateOf(repository.oauthToken != null)
    var userName by mutableStateOf(repository.userName ?: "Local Tracker User")
    var userEmail by mutableStateOf(repository.userEmail ?: "user@example.com")
    var spreadsheetId by mutableStateOf(repository.spreadsheetId)

    var googleClientId by mutableStateOf(repository.googleClientId ?: "")
    var googleClientSecret by mutableStateOf(repository.googleClientSecret ?: "")

    var oauthToken: String?
        get() = repository.oauthToken
        set(value) {
            repository.oauthToken = value
        }

    fun updateProfileName(name: String) {
        userName = name
        repository.userName = name
    }

    fun updateGoogleClientCredentials(clientId: String, clientSecret: String) {
        googleClientId = clientId
        googleClientSecret = clientSecret
        repository.googleClientId = clientId
        repository.googleClientSecret = clientSecret
    }

    fun getOAuthConsentUrl(): String {
        val cid = googleClientId.ifBlank { "703403170183-googleclientid.apps.googleusercontent.com" }
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$cid" +
                "&redirect_uri=com.example.expensetracker://oauth2callback" +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/spreadsheets" +
                "&prompt=consent" +
                "&access_type=offline"
    }

    fun completeOAuthWithCode(code: String) {
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Exchanging Authorization Code for Token..."
            try {
                val token = repository.exchangeCodeForToken(code, googleClientId, googleClientSecret)
                isLoggedIn = true
                val repoVal = repository.userName
                userName = if (repoVal != null && repoVal != "Local Tracker User" && repoVal != "Local Session Tracker") repoVal else "Authorized User"
                userEmail = repository.userEmail ?: "sheets.authorized@gmail.com"
                syncMessage = "Google Sheets successfully authenticated!"
                
                // Try and bootstrap a sheet right away if not present
                if (repository.spreadsheetId == null) {
                    connectOrCreateGoogleSheet()
                }
            } catch (e: Exception) {
                syncMessage = "OAuth Exchange Failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isSyncing = false
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.checkAndBootstrapCategories()
            // Pull if spreadsheet ID is local
            if (isLoggedIn && repository.spreadsheetId != null) {
                pullFromSheets()
            }
        }
    }

    // AUTH ACTIONS (Google sign in simulation matching user flow)
    fun loginWithGoogle(email: String, name: String) {
        val dummyToken = "oauth_token_simulated_for_${email.hashCode()}"
        repository.oauthToken = dummyToken
        repository.userName = name
        repository.userEmail = email
        
        isLoggedIn = true
        userName = name
        userEmail = email
        
        // Automatically fetch if we have an associated spreadsheet ID already
        viewModelScope.launch {
            repository.syncLocalToSheets()
        }
    }

    fun logout() {
        repository.oauthToken = null
        repository.refreshTokenSaved = null
        repository.spreadsheetId = null
        repository.userName = "Local Tracker User"
        repository.userEmail = "user@example.com"

        isLoggedIn = false
        userName = "Local Tracker User"
        userEmail = "user@example.com"
        spreadsheetId = null
    }

    // SHEET SYNC ACTIONS
    fun connectOrCreateGoogleSheet() {
        if (!isLoggedIn) {
            syncMessage = "Please sign in with Google first."
            return
        }
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Creating expense ledger in Google Sheets..."
            try {
                val newSheetId = repository.createGoogleSheet()
                spreadsheetId = newSheetId
                syncMessage = "Ledger connected! Spreadsheet ID: $newSheetId"
            } catch (e: Exception) {
                syncMessage = "Sync failed: Please check credentials or try again"
            } finally {
                isSyncing = false
            }
        }
    }

    fun pullFromSheets() {
        if (!isLoggedIn || spreadsheetId == null) return
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Pulling latest transactions from Google Sheets..."
            try {
                val count = repository.pullDataFromSheets()
                syncMessage = "Synced $count transactions from Google Sheets."
            } catch (e: Exception) {
                syncMessage = "Offline access: displaying local cached transactions"
            } finally {
                isSyncing = false
            }
        }
    }

    fun forcePushLocalToSheets() {
        if (!isLoggedIn || spreadsheetId == null) {
            syncMessage = "Sheets connection not configured"
            return
        }
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Pushing local offline cache to Google spreadsheet..."
            try {
                repository.syncLocalToSheets()
                syncMessage = "Offline changes successfully uploaded!"
            } catch (e: Exception) {
                syncMessage = "Failed: spreadsheet offline sync failed"
            } finally {
                isSyncing = false
            }
        }
    }

    // EXPENSE ACTIONS
    fun addExpense(amount: Double, category: String, description: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                amount = amount,
                category = category,
                date = date,
                description = description,
                synced = false
            )
            repository.saveExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            // also trigger local deletion
            repository.deleteExpenseById(expense.id)
        }
    }

    // CATEGORY ACTIONS
    fun addCategory(name: String, limit: Double, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(name, limit, colorHex, iconName)
            repository.saveCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun deleteCategoryByName(name: String) {
        viewModelScope.launch {
            repository.deleteCategoryByName(name)
        }
    }

    // ANALYTICS CALCULATIONS
    fun getFilteredExpensesList(list: List<ExpenseEntity>): List<ExpenseEntity> {
        return list.filter { item ->
            val matchesCat = selectedCategoryFilter == null || item.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesQuery = searchKeyword.isBlank() || item.description.contains(searchKeyword, ignoreCase = true) || item.category.contains(searchKeyword, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    // Category spends breakdown helper
    val categorySpendMetrics = expenses.map { list ->
        val map = list.groupBy { it.category }
        map.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Category limit warnings & details builder helper
    fun getCategoryBudgetAlerts(
        cats: List<CategoryEntity>,
        spends: Map<String, Double>
    ): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()
        for (c in cats) {
            val spent = spends[c.name] ?: 0.0
            val limit = c.budgetLimit
            if (limit > 0.0) {
                val ratio = spent / limit
                if (ratio >= 1.0) {
                    alerts.add(BudgetAlert(c.name, spent, limit, ratio, AlertLevel.CRITICAL))
                } else if (ratio >= 0.8) {
                    alerts.add(BudgetAlert(c.name, spent, limit, ratio, AlertLevel.WARNING))
                }
            }
        }
        return alerts
    }

    // ViewModel Factory
    class Factory(
        private val application: Application,
        private val repository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class AlertLevel {
    WARNING, CRITICAL
}

data class BudgetAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val ratio: Double,
    val level: AlertLevel
)
