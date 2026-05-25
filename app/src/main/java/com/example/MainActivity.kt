@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.accounts.AccountManager
import android.accounts.Account
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.AlertLevel
import com.example.ui.BudgetAlert
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
 
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(this, database.expenseDao(), database.categoryDao())
        val viewModelFactory = ExpenseViewModel.Factory(application, repository)
 
        viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[ExpenseViewModel::class.java]

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
 
                // Display sync messages as transient indicators
                LaunchedEffect(viewModel.syncMessage) {
                    viewModel.syncMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.syncMessage = null
                    }
                }
 
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!viewModel.isLoggedIn) {
                        LoginScreen(viewModel = viewModel)
                    } else {
                        MainAppContent(viewModel = viewModel, scrollBehavior = scrollBehavior)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.example.expensetracker" && uri.host == "oauth2callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                if (::viewModel.isInitialized) {
                    viewModel.completeOAuthWithCode(code)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf(viewModel.userEmail) }
    var nameInput by remember { mutableStateOf(viewModel.userName) }

    var showAccountChooserDialog by remember { mutableStateOf(false) }
    var dialogStep by remember { mutableStateOf(1) } // 1 = Choose Account, 2 = Permissions Consent

    var selectedEmail by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Brand Header
        Image(
            painter = painterResource(id = R.drawable.finance_app_logo_1779726000241),
            contentDescription = "Expense Tracker Flow Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(18.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Expense Tracker Flow",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The ultimate private ledger to track budgets, alerts, and sync seamlessly with Google Sheets.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Simple button layout
        Button(
            onClick = {
                dialogStep = 1
                showAccountChooserDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_signup_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)) // Google Green
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "Google sign up icon",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sign up with Google",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        TextButton(
            onClick = {
                viewModel.loginWithGoogle("offline@local.app", "Offline Session Explorer")
                viewModel.syncMessage = "Logged in via Sandbox mode"
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("skip_to_offline_button")
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "Offline indicator",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Use Sandbox / Offline Mode Only",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Beautiful Interactive Account Chooser & Permissions Dialog
        if (showAccountChooserDialog) {
            val localAccounts = remember {
                try {
                    val am = AccountManager.get(context)
                    val accounts = am.getAccountsByType("com.google")
                    accounts?.mapNotNull { it?.name }?.distinct() ?: emptyList()
                } catch (t: Throwable) {
                    emptyList<String>()
                }
            }
            // Discovered device accounts + fallbacks
            val mergedAccounts = remember(localAccounts) {
                (localAccounts + listOf("shashikumargundala@gmail.com", "finance.ledger.sync@gmail.com")).distinct()
            }

            Dialog(onDismissRequest = { showAccountChooserDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (dialogStep == 1) {
                            // Account Chooser Step
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Google Accounts",
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Choose an account",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                IconButton(onClick = { showAccountChooserDialog = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "to continue to Expense Tracker Flow",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(mergedAccounts) { accEmail ->
                                    val displayName = if (accEmail.startsWith("shashikumargundala", ignoreCase = true)) {
                                        "Shashikumar Gundala"
                                    } else {
                                        val prefix = accEmail.substringBefore("@").replace(".", " ")
                                        if (prefix.isNotEmpty()) prefix.substring(0, 1).uppercase() + prefix.substring(1) else "Google Session User"
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedEmail = accEmail
                                                selectedName = displayName
                                                dialogStep = 2 // Transition to permissions consent!
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F4)),
                                        border = BorderStroke(1.dp, Color(0xFFDADCE0))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1A73E8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = accEmail.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                                Text(
                                                    text = accEmail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = "Select account",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your personal details are secured using standard sandbox rules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                        } else {
                            // Permissions Consent Step
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VpnKey,
                                        contentDescription = "Permissions Required",
                                        tint = Color(0xFFE28743),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google Account Consent",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                IconButton(onClick = { dialogStep = 1 }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Go back", tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Show active user profile logo & details
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                                border = BorderStroke(1.dp, Color(0xFF1A73E8))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1A73E8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedEmail.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = selectedName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = selectedEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Expense Tracker Flow wants to access your Google Account. This is required for sync integration:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bullet 1: Sheets Permissions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF34A853))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Manage your Google Sheets spreadsheets",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Allows creating and writing budget ledger rows directly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bullet 2: Profile Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4285F4))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "View your personal profile details",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Retrieving display name and email address securely.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "By tapping Allow, you agree that your selected Google account information will be synchronized securely under compliance standards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { dialogStep = 1 }
                                ) {
                                    Text("Back", color = Color.Gray, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        emailInput = selectedEmail
                                        nameInput = selectedName
                                        showAccountChooserDialog = false
                                        viewModel.loginWithGoogle(selectedEmail, selectedName)
                                        viewModel.syncMessage = "Authenticated Google User: $selectedEmail"
                                        // Auto trigger sync/connect
                                        viewModel.connectOrCreateGoogleSheet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Allow", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ExpenseViewModel, scrollBehavior: TopAppBarScrollBehavior) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categorySpends by viewModel.categorySpendMetrics.collectAsStateWithLifecycle()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showCategoryLimitDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }

    val filteredList = remember(expenses, viewModel.selectedCategoryFilter, viewModel.searchKeyword) {
        viewModel.getFilteredExpensesList(expenses)
    }

    val spendAlerts = remember(categories, categorySpends) {
        viewModel.getCategoryBudgetAlerts(categories, categorySpends)
    }

    val coroutineScope = rememberCoroutineScope()

    val initials = remember(viewModel.userName) {
        val name = viewModel.userName.trim()
        if (name.isEmpty()) "AM"
        else {
            val parts = name.split("\\s+".toRegex())
            if (parts.size >= 2) {
                "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            } else {
                name.take(2).uppercase()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProfileSettingsDialog = true }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Welcome back",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = viewModel.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add, 
                    contentDescription = "Create entry",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google Sheets connectivity panel & stats card
            item {
                SheetsIntegrationCard(viewModel = viewModel)
            }

            // Spending Trend Visualizations (Pie/Donut and Bar lists)
            item {
                SpendingAnalyticsCard(
                    categorySpends = categorySpends,
                    categories = categories
                )
            }

            // Budget Alerts Section
            if (spendAlerts.isNotEmpty()) {
                item {
                    BudgetAlertsSection(alerts = spendAlerts)
                }
            }

            // Quick Filters (Categories list + customizable categories controller badge)
            item {
                CategoryFiltersSection(
                    categories = categories,
                    selectedFilter = viewModel.selectedCategoryFilter,
                    onSelectFilter = { viewModel.selectedCategoryFilter = it },
                    onManageCategories = { showCategoryLimitDialog = true }
                )
            }

            // Search Bar Input
            item {
                OutlinedTextField(
                    value = viewModel.searchKeyword,
                    onValueChange = { viewModel.searchKeyword = it },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (viewModel.searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchKeyword = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_input"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            // Transactions Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Ledger Rows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Viewing ${filteredList.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Transactions list
            if (filteredList.isEmpty()) {
                item {
                    EmptyExpensesPlaceholder()
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    ExpenseTableRow(
                        expense = item,
                        categoryColorHex = categories.find { it.name.equals(item.category, ignoreCase = true) }?.colorHex ?: "#9E9E9E",
                        onDelete = { viewModel.deleteExpense(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Modal dialog for adding entries
    if (showAddExpenseDialog) {
        ExpenseInputDialog(
            categories = categories,
            onDismiss = { showAddExpenseDialog = false },
            onConfirmAdd = { amount, category, desc ->
                viewModel.addExpense(amount, category, desc)
                showAddExpenseDialog = false
            }
        )
    }

    // Modal dialog for managing categories and budgets
    if (showCategoryLimitDialog) {
        CategorySettingsDialog(
            categories = categories,
            onDismiss = { showCategoryLimitDialog = false },
            onSaveNewCategory = { name, limit, colorHex, iconName ->
                viewModel.addCategory(name, limit, colorHex, iconName)
            },
            onDeleteCategory = { name ->
                viewModel.deleteCategoryByName(name)
            }
        )
    }

    // Modal dialog for updating Gmail display name from Profile Settings
    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(
            currentName = viewModel.userName,
            currentEmail = viewModel.userEmail,
            onDismiss = { showProfileSettingsDialog = false },
            onSaveName = { updatedName ->
                viewModel.updateProfileName(updatedName)
            }
        )
    }
}

@Composable
fun ProfileSettingsDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit
) {
    var editedName by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("profile_settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close profile settings")
                    }
                }

                Text(
                    text = "Gmail Account details are synchronized automatically and cannot be altered. You can, however, update your public display name locally below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = currentEmail,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Associated Gmail") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "EmailIcon") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Your Display Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "NameIcon") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_edit_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                onSaveName(editedName)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun SheetsIntegrationCard(viewModel: ExpenseViewModel) {
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = "Sheets Logo",
                        tint = Color(0xFF0F9D58), // Sheets Green
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Sheets Vault",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Cloud Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (viewModel.spreadsheetId != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewModel.spreadsheetId != null) Icons.Filled.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = "Cloud Status",
                        tint = if (viewModel.spreadsheetId != null) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (viewModel.spreadsheetId != null) "Connected" else "Local Only",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.spreadsheetId != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewModel.spreadsheetId == null) {
                Text(
                    text = "Persist transactions in real-time to a secure spreadsheet book in Google Drive, and import rows for offline analytics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.connectOrCreateGoogleSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_sheet_button"),
                    enabled = !viewModel.isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                ) {
                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AddToDrive, contentDescription = "Add Google Sheets", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create & Sync Google Sheet", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Connected info banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Sheet connected successfully:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.spreadsheetId ?: "",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.pullFromSheets() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pull_sheet_button"),
                        enabled = !viewModel.isSyncing,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Import sheets", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh Pull", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { viewModel.forcePushLocalToSheets() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("push_sheet_button"),
                        enabled = !viewModel.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = "Push rows", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Force Push", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingAnalyticsCard(
    categorySpends: Map<String, Double>,
    categories: List<CategoryEntity>
) {
    val totalSpend = categorySpends.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Spending Trends",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (totalSpend == 0.0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.InsertChartOutlined,
                        contentDescription = "Chart placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Visual spending habits and allocation slices display automatically here once elements are added.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                // Chart Canvas block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Empty track background
                            drawArc(
                                color = Color(0xFFE7E0EC),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 30f, cap = StrokeCap.Round)
                            )

                            var startAngle = -90f
                            for (cat in categories) {
                                val amount = categorySpends[cat.name] ?: 0.0
                                if (amount > 0.0) {
                                    val sweep = (amount / totalSpend * 360f).toFloat()
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (e: Exception) {
                                        Color.Gray
                                    }
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 30f, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("$%.0f", totalSpend),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TOTAL EXP.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legending columns detailing percentage weights
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.filter { (categorySpends[it.name] ?: 0.0) > 0.0 }
                            .take(5) // Limit legend to top 5 categories
                            .forEach { cat ->
                                val amt = categorySpends[cat.name] ?: 0.0
                                val pct = (amt / totalSpend * 100).toInt()
                                val color = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Comparative category analysis meters (Horizontal meters)
                Text(
                    text = "Comparative Category Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val amt = categorySpends[cat.name] ?: 0.0
                        val limit = cat.budgetLimit
                        val color = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.name, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (limit > 0.0) String.format("$%.0f / $%.0f", amt, limit) else String.format("$%.0f", amt),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Row meter drawing
                            val ratio = if (limit > 0.0) (amt / limit).toFloat().coerceIn(0f, 1f) else (if (totalSpend > 0.0) (amt / totalSpend).toFloat() else 0f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (limit > 0.0 && amt > limit) MaterialTheme.colorScheme.error else color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetAlertsSection(alerts: List<BudgetAlert>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        alerts.forEach { alert ->
            val txt = if (alert.level == AlertLevel.CRITICAL) {
                "Exceeded budget for ${alert.category}! Spent $${String.format("%.2f", alert.spent)} of $${String.format("%.2f", alert.limit)}"
            } else {
                "Nearing limit for ${alert.category} (${(alert.ratio * 100).toInt()}%). Spent $${String.format("%.2f", alert.spent)} of $${String.format("%.2f", alert.limit)}"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .drawBehind {
                        val strokePx = 4.dp.toPx()
                        drawLine(
                            color = Color(0xFFB3261E), // error red from HTML
                            start = androidx.compose.ui.geometry.Offset(strokePx / 2, 0f),
                            end = androidx.compose.ui.geometry.Offset(strokePx / 2, size.height),
                            strokeWidth = strokePx
                        )
                    }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Margin spacer for left border highlight
                    Spacer(modifier = Modifier.width(2.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Alert Symbol",
                            tint = Color(0xFFB3261E), // Red warning text/icon matching HTML
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Monthly Budget Alert",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF31111D), // Dark tone for secondary text matching HTML
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = txt,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF31111D).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFiltersSection(
    categories: List<CategoryEntity>,
    selectedFilter: String?,
    onSelectFilter: (String?) -> Unit,
    onManageCategories: () -> Unit
) {
    Column {
        Text(
            text = "Categories & Limits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text("All") },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }

                items(categories, key = { it.name }) { cat ->
                    FilterChip(
                        selected = selectedFilter.equals(cat.name, ignoreCase = true),
                        onClick = { onSelectFilter(if (selectedFilter.equals(cat.name, ignoreCase = true)) null else cat.name) },
                        label = { Text(cat.name) },
                        modifier = Modifier.testTag("filter_chip_${cat.name.lowercase()}")
                    )
                }
            }

            // Manage buttons
            FilledIconButton(
                onClick = onManageCategories,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("manage_categories_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Customize categories",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ExpenseTableRow(
    expense: ExpenseEntity,
    categoryColorHex: String,
    onDelete: () -> Unit
) {
    val dateText = remember(expense.date) {
        val diffMs = System.currentTimeMillis() - expense.date
        when {
            diffMs < 60000 -> "Just now"
            diffMs < 3600000 -> "${diffMs / 60000}m ago"
            diffMs < 86400000 -> "${diffMs / 3600000}h ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
        }
    }

    val catColor = remember(categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(categoryColorHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val iconVector = remember(expense.category) {
        when (expense.category.lowercase(Locale.getDefault()).trim()) {
            "food" -> Icons.Filled.Restaurant
            "travel" -> Icons.Filled.Flight
            "personal" -> Icons.Filled.Fingerprint
            "entertainment", "leisure" -> Icons.Filled.Celebration
            "subscriptions", "bills" -> Icons.Filled.ReceiptLong
            "shopping" -> Icons.Filled.ShoppingBag
            else -> Icons.Filled.Wallet
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge inside padded container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = expense.category,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { "Generic transaction" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sum and Sheet Sync Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("-$%.2f", expense.amount),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expense.synced) Icons.Filled.CheckCircle else Icons.Outlined.CloudOff,
                        contentDescription = "Sync State Indicator",
                        tint = if (expense.synced) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Trash log",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyExpensesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Receipt,
            contentDescription = "Receipt placeholder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No recorded logs meet filter constraints.",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap the bottom-right + FAB to log a new expense row.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ExpenseInputDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirmAdd: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Personal") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_expense_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Record Spending Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expenditure Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // Customizable Dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Label Category") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .testTag("expense_category_selector"),
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(Color.White)
                    ) {
                        categories.forEach { cat ->
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isHovered by itemInteractionSource.collectIsHoveredAsState()
                            val isPressed by itemInteractionSource.collectIsPressedAsState()
                            val itemBgColor = if (isHovered || isPressed) Color(0xFFF1F1F1) else Color.White

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat.name,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat.name
                                    dropdownExpanded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBgColor),
                                interactionSource = itemInteractionSource,
                                colors = MenuDefaults.itemColors(
                                    textColor = Color.Black,
                                    leadingIconColor = Color.Black,
                                    trailingIconColor = Color.Black
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Short Description") },
                    placeholder = { Text("Gas, grocery shopping, restaurant billing, etc.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_desc_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("expense_cancel_btn")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onConfirmAdd(amt, selectedCategory, descriptionText)
                            }
                        },
                        modifier = Modifier.testTag("expense_confirm_btn")
                    ) {
                        Text("Save log")
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySettingsDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSaveNewCategory: (String, Double, String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var newCatLimit by remember { mutableStateOf("") }
    var selectedColorIdx by remember { mutableStateOf(0) }

    val presetColors = listOf(
        "#9C27B0", // Purple
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#009688", // Teal
        "#2196F3", // Blue
        "#E91E63", // Pink
        "#795548", // Brown
        "#607D8B", // Slate
        "#FF5722"  // Coral
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("category_settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Cancel, contentDescription = "Close settings")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Create a Tag Form
                Text(
                    text = "Add Custom Budget Tag label",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name (e.g., Subscriptions)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_category_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                OutlinedTextField(
                    value = newCatLimit,
                    onValueChange = { newCatLimit = it },
                    label = { Text("Monthly Budget Limit ($) — 0 for none") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_category_limit_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // Horizontal Color picker circle row
                Text(
                    text = "Pick Tag Color Accent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetColors.size) { idx ->
                        val hex = presetColors[idx]
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorIdx = idx }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColorIdx == idx) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val limitVal = newCatLimit.toDoubleOrNull() ?: 0.0
                            val col = presetColors[selectedColorIdx]
                            onSaveNewCategory(newCatName.trim(), limitVal, col, "Category")
                            newCatName = ""
                            newCatLimit = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_category_btn")
                ) {
                    Text("Add New Custom Tag", fontWeight = FontWeight.Bold)
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Scrollable category list with limits
                Text(
                    text = "Existing Active Tags:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(modifier = Modifier.height(180.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categories, key = { it.name }) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (cat.budgetLimit > 0.0) "Budget: $${cat.budgetLimit}" else "No active limit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteCategory(cat.name) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_category_${cat.name.lowercase()}"),
                                    enabled = categories.size > 1 // prevent empty constraints
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete tag",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
