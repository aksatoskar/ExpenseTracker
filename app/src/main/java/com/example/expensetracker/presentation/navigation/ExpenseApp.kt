package com.example.expensetracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.presentation.AppViewModel
import com.example.expensetracker.presentation.analytics.AnalyticsScreen
import com.example.expensetracker.presentation.budget.BudgetDetailScreen
import com.example.expensetracker.presentation.budget.BudgetScreen
import com.example.expensetracker.presentation.dashboard.AddTransactionDialog
import com.example.expensetracker.presentation.dashboard.DashboardScreen
import com.example.expensetracker.presentation.dashboard.DashboardViewModel
import com.example.expensetracker.presentation.onboarding.OnboardingScreen
import com.example.expensetracker.presentation.review.ReviewDialog
import com.example.expensetracker.presentation.review.ReviewViewModel
import com.example.expensetracker.presentation.settings.DetectedMessagesScreen
import com.example.expensetracker.presentation.settings.SettingsScreen
import com.example.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.example.expensetracker.presentation.transactions.TransactionsScreen

/**
 * Root composable: applies the theme, gates onboarding, hosts the bottom-nav shell and renders
 * the review dialog and budget-detail overlays driven by lightweight local navigation state.
 */
@Composable
fun ExpenseApp(startReviewId: Long?) {
    val appVm: AppViewModel = hiltViewModel()
    val state by appVm.uiState.collectAsState()
    var reviewId by remember { mutableStateOf(startReviewId) }
    var budgetCategory by remember { mutableStateOf<Category?>(null) }
    var showDetectedMessages by remember { mutableStateOf(false) }

    ExpenseTrackerTheme(darkTheme = state.darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                !state.loaded -> Unit
                !state.onboardingComplete -> OnboardingScreen(onDone = appVm::completeOnboarding)
                else -> {
                    LaunchedEffect(Unit) { appVm.runStartupTasks() }
                    val showSyncPrompt by appVm.showSyncPrompt.collectAsState()
                    AppShell(
                        openReview = { reviewId = it },
                        openBudget = { budgetCategory = it },
                        openDetectedMessages = { showDetectedMessages = true },
                        logScreen = appVm::logScreen
                    )
                    if (showSyncPrompt) {
                        AlertDialog(
                            onDismissRequest = { appVm.resolveSyncPrompt(sync = false) },
                            title = { Text("Sync your data?") },
                            text = {
                                Text(
                                    "Back up your transactions, budgets and reports to your Google account. " +
                                        "You can sync any time from Settings."
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { appVm.resolveSyncPrompt(sync = true) }) { Text("Sync now") }
                            },
                            dismissButton = {
                                TextButton(onClick = { appVm.resolveSyncPrompt(sync = false) }) { Text("Not now") }
                            }
                        )
                    }
                    budgetCategory?.let { cat ->
                        BudgetDetailScreen(
                            category = cat,
                            onBack = { budgetCategory = null },
                            openReview = { reviewId = it }
                        )
                    }
                    if (showDetectedMessages) {
                        DetectedMessagesScreen(onBack = { showDetectedMessages = false })
                    }
                    reviewId?.let { id ->
                        val reviewVm: ReviewViewModel = hiltViewModel()
                        val flow = remember(id) { reviewVm.observeTransaction(id) }
                        val transaction by flow.collectAsState(initial = null)
                        transaction?.let { txn ->
                            ReviewDialog(
                                transaction = txn,
                                onSave = { amountPaise, category, priority, notes ->
                                    reviewVm.save(txn, amountPaise, category, priority, notes)
                                    reviewId = null
                                },
                                onDelete = {
                                    reviewVm.delete(txn.id)
                                    reviewId = null
                                },
                                onDismiss = { reviewId = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShell(
    openReview: (Long) -> Unit,
    openBudget: (Category) -> Unit,
    openDetectedMessages: () -> Unit,
    logScreen: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddTransaction by remember { mutableStateOf(false) }
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val tabs = listOf("Home", "Txns", "Charts", "Budget", "Settings")
    val icons = listOf(Icons.Default.Dashboard, Icons.Default.ReceiptLong, Icons.Default.Analytics, Icons.Default.Payments, Icons.Default.Settings)
    LaunchedEffect(tab) { logScreen(tabs[tab]) }
    Scaffold(
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(
                    onClick = { showAddTransaction = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = {
                            Text(title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> DashboardScreen(openReview)
                1 -> TransactionsScreen(openReview)
                2 -> AnalyticsScreen()
                3 -> BudgetScreen(openBudget)
                else -> SettingsScreen(onOpenDetectedMessages = openDetectedMessages)
            }
        }
    }
    if (showAddTransaction) {
        AddTransactionDialog(
            onAdd = { amount, merchant, category, priority, notes ->
                dashboardVm.addManual(amount, merchant, category, priority, notes)
            },
            onDismiss = { showAddTransaction = false }
        )
    }
}
