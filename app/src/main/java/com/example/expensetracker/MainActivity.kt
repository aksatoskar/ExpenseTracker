package com.example.expensetracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.AmountByCategory
import com.example.expensetracker.data.AmountByMerchant
import com.example.expensetracker.data.AmountByPriority
import com.example.expensetracker.data.BudgetEntity
import com.example.expensetracker.data.BudgetHistoryEntity
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.DateRange
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.Priority
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionStatus
import com.example.expensetracker.domain.formatInr
import com.example.expensetracker.domain.rupeesToPaise
import com.example.expensetracker.notifications.ExpenseNotifications
import com.example.expensetracker.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reviewId = intent.getLongExtra(EXTRA_REVIEW_ID, -1L)
        setContent {
            ExpenseApp(startReviewId = reviewId.takeIf { it > 0 })
        }
    }

    companion object {
        private const val EXTRA_REVIEW_ID = "review_transaction_id"
        fun intent(context: Context, reviewTransactionId: Long? = null): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { reviewTransactionId?.let { putExtra(EXTRA_REVIEW_ID, it) } }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(app: ExpenseTrackerApp) : AndroidViewModel(app) {
    private val repository: ExpenseRepository = app.repository
    private val prefs = app.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    private val range = mutableStateOf(DateRange.month())

    val onboardingComplete = mutableStateOf(prefs.getBoolean("onboarding_complete", false))
    val darkTheme = mutableStateOf(prefs.getBoolean("dark_theme", false))
    val lastSmsSync = mutableStateOf(prefs.getLong("last_sms_sync", 0L))
    val isSyncing = mutableStateOf(false)
    val selectedRange = mutableStateOf("Month")
    val latest = repository.latestTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val all = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pending = repository.pendingTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reports = repository.reports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val budgetHistory = repository.budgetHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val spendingTotals = combine(
        repository.observeTotal(DateRange.today()),
        repository.observeTotal(DateRange.week()),
        repository.observeTotal(DateRange.month()),
        repository.observeTotal(DateRange.lastWeek())
    ) { today, week, month, lastWeek -> SpendingTotals(today, week, month, lastWeek) }

    val dashboard: StateFlow<DashboardState> = combine(
        spendingTotals,
        repository.observeCategoryTotals(DateRange.month()),
        repository.observePriorityTotals(DateRange.month()),
        repository.pendingCount
    ) { totals, categoryTotals, priorityTotals, pendingCount ->
        DashboardState(totals.today, totals.week, totals.month, totals.lastWeek, categoryTotals, priorityTotals, pendingCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    val analytics: StateFlow<AnalyticsState> =
        snapshotFlow { range.value }
            .flatMapLatest { current ->
                combine(
                    repository.observeCategoryTotals(current),
                    repository.observeTopMerchants(current),
                    repository.observeTotal(current)
                ) { categories, merchants, total -> AnalyticsState(categories, merchants, total) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        onboardingComplete.value = true
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
        darkTheme.value = enabled
    }

    fun syncSms(onComplete: (Int) -> Unit = {}) {
        if (isSyncing.value) return
        val context = getApplication<ExpenseTrackerApp>()
        if (!PermissionHelper.hasReadSmsPermission(context)) {
            onComplete(-1)
            return
        }
        isSyncing.value = true
        viewModelScope.launch {
            val since = maxOf(lastSmsSync.value, installTimeMillis(context))
            val now = System.currentTimeMillis()
            val count = try {
                withContext(Dispatchers.IO) { repository.syncSmsInbox(since) }
            } catch (e: Exception) {
                0
            }
            prefs.edit().putLong("last_sms_sync", now).apply()
            lastSmsSync.value = now
            isSyncing.value = false
            onComplete(count)
        }
    }

    fun setRange(label: String) {
        selectedRange.value = label
        range.value = when (label) {
            "Today" -> DateRange.today()
            "Week" -> DateRange.week()
            else -> DateRange.month()
        }
    }

    fun saveReview(transaction: TransactionEntity, category: Category, priority: Priority, notes: String) {
        viewModelScope.launch { repository.saveReview(transaction, category, priority, notes) }
    }

    fun skip(transaction: TransactionEntity) {
        viewModelScope.launch { repository.skip(transaction) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun saveBudget(category: Category, amountRupees: String, existing: BudgetEntity?) {
        val amount = amountRupees.toDoubleOrNull() ?: return
        viewModelScope.launch { repository.upsertBudget(category, rupeesToPaise(amount), existing) }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch { repository.deleteBudget(budget.id) }
    }

    fun renewBudgetsIfNeeded() {
        viewModelScope.launch {
            val key = "last_budget_archive_month"
            val current = YearMonth.now()
            val stored = prefs.getString(key, null)
            val lastArchived = stored?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

            if (lastArchived == null) {
                prefs.edit().putString(key, current.minusMonths(1).toString()).apply()
                return@launch
            }

            var cursor = lastArchived.plusMonths(1)
            var archivedAny = false
            while (cursor.isBefore(current)) {
                repository.archiveMonth(cursor)
                archivedAny = true
                cursor = cursor.plusMonths(1)
            }
            prefs.edit().putString(key, current.minusMonths(1).toString()).apply()
            if (archivedAny) repository.resetAllBudgetAlertFlags()
        }
    }

    fun observeTransaction(id: Long) = repository.observeTransaction(id)

    private fun installTimeMillis(context: Context): Long =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(System.currentTimeMillis())
}

data class DashboardState(
    val todayPaise: Long = 0,
    val weekPaise: Long = 0,
    val monthPaise: Long = 0,
    val lastWeekPaise: Long = 0,
    val categoryTotals: List<AmountByCategory> = emptyList(),
    val priorityTotals: List<AmountByPriority> = emptyList(),
    val pendingCount: Int = 0
)

data class SpendingTotals(val today: Long, val week: Long, val month: Long, val lastWeek: Long)

data class AnalyticsState(
    val categories: List<AmountByCategory> = emptyList(),
    val merchants: List<AmountByMerchant> = emptyList(),
    val totalPaise: Long = 0
)

@Composable
fun ExpenseApp(startReviewId: Long?) {
    val context = LocalContext.current
    val vm: ExpenseViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(context.applicationContext as ExpenseTrackerApp) as T
            }
        }
    )
    var reviewId by remember { mutableStateOf(startReviewId) }
    val onboardingComplete by vm.onboardingComplete
    val darkTheme by vm.darkTheme

    ExpenseTrackerTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!onboardingComplete) {
                OnboardingScreen(onDone = vm::completeOnboarding)
            } else {
                LaunchedEffect(Unit) {
                    vm.syncSms()
                    vm.renewBudgetsIfNeeded()
                }
                AppShell(vm, openReview = { reviewId = it })
                reviewId?.let { id ->
                    val transaction by vm.observeTransaction(id).collectAsState(initial = null)
                    transaction?.let {
                        ReviewDialog(
                            transaction = it,
                            onSave = { category, priority, notes ->
                                vm.saveReview(it, category, priority, notes)
                                reviewId = null
                            },
                            onSkip = {
                                vm.skip(it)
                                reviewId = null
                            },
                            onDelete = {
                                vm.delete(it.id)
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

@Composable
fun ExpenseTrackerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF6D28D9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95),
        secondary = Color(0xFF0891B2),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFFAFE),
        onSecondaryContainer = Color(0xFF164E63),
        tertiary = Color(0xFFEA580C),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFEDD5),
        onTertiaryContainer = Color(0xFF9A3412),
        background = Color(0xFFF8F5FF),
        onBackground = Color(0xFF1E1B2E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1E1B2E),
        surfaceVariant = Color(0xFFEDE8F5),
        onSurfaceVariant = Color(0xFF5B5470),
        error = Color(0xFFDC2626),
        errorContainer = Color(0xFFFEE2E2)
    )
    val darkColors = darkColorScheme(
        primary = Color(0xFFA78BFA),
        onPrimary = Color(0xFF2E1065),
        primaryContainer = Color(0xFF4C1D95),
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF22D3EE),
        onSecondary = Color(0xFF083344),
        secondaryContainer = Color(0xFF155E75),
        onSecondaryContainer = Color(0xFFCFFAFE),
        tertiary = Color(0xFFFB923C),
        onTertiary = Color(0xFF431407),
        tertiaryContainer = Color(0xFF9A3412),
        onTertiaryContainer = Color(0xFFFFEDD5),
        background = Color(0xFF0F0D17),
        onBackground = Color(0xFFF3EEFF),
        surface = Color(0xFF1A1628),
        onSurface = Color(0xFFF3EEFF),
        surfaceVariant = Color(0xFF2D2640),
        onSurfaceVariant = Color(0xFFC9BFD9),
        error = Color(0xFFF87171),
        errorContainer = Color(0xFF7F1D1D)
    )
    MaterialTheme(colorScheme = if (darkTheme) darkColors else lightColors, content = content)
}

private val chartColors = listOf(
    Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF59E0B),
    Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF06B6D4), Color(0xFF84CC16)
)

private val priorityEssentialColor = Color(0xFF059669)
private val priorityOptionalColor = Color(0xFFEA580C)
private val priorityWastefulColor = Color(0xFFEC4899)

private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.Essential -> priorityEssentialColor
    Priority.Optional -> priorityOptionalColor
    Priority.Wasteful -> priorityWastefulColor
}

private fun categoryColor(category: Category?): Color = when (category) {
    Category.FoodDining -> Color(0xFFEF4444)
    Category.Shopping -> Color(0xFFEC4899)
    Category.Travel -> Color(0xFF06B6D4)
    Category.BillsUtilities -> Color(0xFF6366F1)
    Category.RentHome -> Color(0xFF8B5CF6)
    Category.Health -> Color(0xFF10B981)
    Category.Education -> Color(0xFF3B82F6)
    Category.Investments -> Color(0xFF059669)
    Category.Entertainment -> Color(0xFFF59E0B)
    Category.Other, null -> Color(0xFF94A3B8)
}

private fun categoryIcon(category: Category?): ImageVector = when (category) {
    Category.FoodDining -> Icons.Default.Restaurant
    Category.Shopping -> Icons.Default.ShoppingCart
    Category.Travel -> Icons.Default.Flight
    Category.BillsUtilities -> Icons.Default.ReceiptLong
    Category.RentHome -> Icons.Default.Home
    Category.Health -> Icons.Default.LocalHospital
    Category.Education -> Icons.Default.School
    Category.Investments -> Icons.Default.TrendingUp
    Category.Entertainment -> Icons.Default.Movie
    Category.Other, null -> Icons.Default.Category
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onDone()
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        step = 2
    }

    val permissions = listOf(
        Triple("Notification Access", "Allows the app to detect payment notifications from UPI and banking apps.", "Open Settings"),
        Triple("SMS Access", "Allows local parsing of bank SMS messages for automatic expense detection.", "Allow SMS"),
        Triple("Notification Permission", "Allows the app to show review prompts, reminders, digests, and reports.", "Allow Notifications")
    )
    val current = permissions[step.coerceAtMost(2)]

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Track your expenses automatically from SMS and payment notifications.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            listOf("Automatic transaction detection", "Smart categorization", "Spending insights", "Budget tracking").forEach {
                Text("• $it", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(32.dp))
            if (step > 0) {
                Text("Permission $step of 3", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(current.first, style = MaterialTheme.typography.titleLarge)
            Text(current.second, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when (step) {
                    0 -> {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        step = 1
                    }
                    1 -> smsLauncher.launch(PermissionHelper.smsPermissions)
                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onDone()
                        }
                    }
                }
            }
        ) {
            Text(if (step == 0) "Get Started" else current.third)
        }
    }
}

@Composable
fun AppShell(vm: ExpenseViewModel, openReview: (Long) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Txns", "Charts", "Budget", "Settings")
    val icons = listOf(Icons.Default.Dashboard, Icons.Default.ReceiptLong, Icons.Default.Analytics, Icons.Default.Payments, Icons.Default.Settings)
    Scaffold(
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
                            Text(
                                title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
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
                0 -> DashboardScreen(vm, openReview)
                1 -> TransactionsScreen(vm, openReview)
                2 -> AnalyticsScreen(vm)
                3 -> BudgetScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
fun SyncStatusCard(vm: ExpenseViewModel) {
    val context = LocalContext.current
    val lastSync by vm.lastSmsSync
    val syncing by vm.isSyncing
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("SMS Sync", fontWeight = FontWeight.SemiBold)
                Text(
                    "Last synced: ${syncTimeText(lastSync)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Button(
                    onClick = {
                        vm.syncSms { count ->
                            val message = when {
                                count < 0 -> "Enable SMS access to sync"
                                count == 0 -> "No new transactions found"
                                count == 1 -> "1 new transaction added to review"
                                else -> "$count new transactions added to review"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sync now")
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(vm: ExpenseViewModel, openReview: (Long) -> Unit) {
    val dashboard by vm.dashboard.collectAsState()
    val latest by vm.latest.collectAsState()
    val pending by vm.pending.collectAsState()
    val budgets by vm.budgets.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DashboardHero(dashboard) }
        item { SyncStatusCard(vm) }
        if (dashboard.pendingCount > 0) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable { pending.firstOrNull()?.let { openReview(it.id) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "You have ${dashboard.pendingCount} uncategorized transactions.",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Review Now",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item { SectionHeader("Where it goes") }
        item { PriorityBreakdownCard(dashboard.priorityTotals) }
        item { SectionHeader("Smart Insights") }
        item { InsightList(dashboard, budgets) }
        item { SectionHeader("Recent Transactions") }
        items(latest) { TransactionRow(it, onClick = { openReview(it.id) }) }
    }
}

@Composable
private fun DashboardHero(state: DashboardState) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            )
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Spent this month",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                formatInr(state.monthPaise),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStatPill("Today", state.todayPaise, Modifier.weight(1f))
                HeroStatPill("This week", state.weekPaise, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStatPill(label: String, amountPaise: Long, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(2.dp))
        Text(
            formatInr(amountPaise),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun PriorityBreakdownCard(priorityTotals: List<AmountByPriority>) {
    val essential = priorityTotals.firstOrNull { it.priority == Priority.Essential }?.amountPaise ?: 0L
    val optional = priorityTotals.firstOrNull { it.priority == Priority.Optional }?.amountPaise ?: 0L
    val wasteful = priorityTotals.firstOrNull { it.priority == Priority.Wasteful }?.amountPaise ?: 0L
    val total = essential + optional + wasteful
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (total <= 0) {
                Text(
                    "No categorized spending yet this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    if (essential > 0) Box(Modifier.weight(essential.toFloat()).fillMaxHeight().background(priorityEssentialColor))
                    if (optional > 0) Box(Modifier.weight(optional.toFloat()).fillMaxHeight().background(priorityOptionalColor))
                    if (wasteful > 0) Box(Modifier.weight(wasteful.toFloat()).fillMaxHeight().background(priorityWastefulColor))
                }
                Spacer(Modifier.height(14.dp))
                PriorityLegendRow("Essential", essential, total, priorityEssentialColor)
                Spacer(Modifier.height(8.dp))
                PriorityLegendRow("Optional", optional, total, priorityOptionalColor)
                Spacer(Modifier.height(8.dp))
                PriorityLegendRow("Wasteful", wasteful, total, priorityWastefulColor)
            }
        }
    }
}

@Composable
private fun PriorityLegendRow(label: String, amountPaise: Long, total: Long, color: Color) {
    val pct = if (total > 0) (amountPaise * 100 / total).toInt() else 0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "$pct%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            formatInr(amountPaise),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private data class Insight(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val color: Color
)

private fun buildInsights(state: DashboardState, budgets: List<BudgetEntity>): List<Insight> {
    val total = state.monthPaise
    if (total <= 0L) {
        return listOf(
            Insight(
                Icons.Default.Lightbulb,
                "Nothing tracked yet",
                "Your spending insights for this month will appear here.",
                Color(0xFF6366F1)
            )
        )
    }
    val insights = mutableListOf<Insight>()

    state.categoryTotals
        .filter { it.category != null }
        .maxByOrNull { it.amountPaise }
        ?.let { top ->
            val cat = top.category!!
            val pct = (top.amountPaise * 100 / total).toInt()
            insights.add(
                Insight(
                    categoryIcon(cat),
                    "Top category",
                    "${cat.label} • ${formatInr(top.amountPaise)} ($pct%)",
                    categoryColor(cat)
                )
            )
        }

    budgets
        .mapNotNull { budget ->
            val spent = state.categoryTotals.firstOrNull { it.category == budget.category }?.amountPaise ?: 0L
            val pct = (spent * 100 / budget.limitPaise.coerceAtLeast(1)).toInt()
            if (pct >= 90) Pair(budget, pct) else null
        }
        .maxByOrNull { it.second }
        ?.let { (budget, pct) ->
            val over = pct >= 100
            insights.add(
                Insight(
                    Icons.Default.Warning,
                    if (over) "Budget exceeded" else "Budget almost used",
                    "${budget.category.label} at $pct% of limit",
                    Color(0xFFEF4444)
                )
            )
        }

    val week = state.weekPaise
    val lastWeek = state.lastWeekPaise
    if (lastWeek > 0L) {
        val change = ((week - lastWeek) * 100 / lastWeek).toInt()
        val up = week >= lastWeek
        insights.add(
            Insight(
                if (up) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                if (up) "Spending up vs last week" else "Spending down vs last week",
                "${kotlin.math.abs(change)}% • ${formatInr(week)} this week",
                if (up) Color(0xFFEA580C) else Color(0xFF059669)
            )
        )
    } else if (week > 0L) {
        insights.add(
            Insight(
                Icons.Default.TrendingUp,
                "Spending this week",
                formatInr(week),
                Color(0xFF6366F1)
            )
        )
    }

    val wasteful = state.priorityTotals.firstOrNull { it.priority == Priority.Wasteful }?.amountPaise ?: 0L
    if (wasteful > 0L) {
        val pct = (wasteful * 100 / total).toInt()
        insights.add(
            Insight(
                Icons.Default.MoneyOff,
                "Wasteful spending",
                "$pct% of this month • ${formatInr(wasteful)}",
                priorityWastefulColor
            )
        )
    } else {
        insights.add(
            Insight(
                Icons.Default.CheckCircle,
                "No wasteful spending",
                "Nicely done — every rupee had a purpose.",
                Color(0xFF059669)
            )
        )
    }

    return insights.take(4)
}

@Composable
fun InsightList(state: DashboardState, budgets: List<BudgetEntity>) {
    val insights = buildInsights(state, budgets)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        insights.forEach { insight ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(insight.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            insight.icon,
                            contentDescription = null,
                            tint = insight.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            insight.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(vm: ExpenseViewModel, openReview: (Long) -> Unit) {
    val all by vm.all.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = all.filter { it.merchant.contains(query, ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search merchant") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
        items(filtered) { TransactionRow(it, onClick = { openReview(it.id) }) }
    }
}

@Composable
fun TransactionRow(transaction: TransactionEntity, onClick: () -> Unit) {
    val accent = categoryColor(transaction.category)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(56.dp)
                    .background(accent)
            )
            Row(
                Modifier.padding(14.dp).weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(transaction.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                transaction.category?.label ?: "Uncategorized",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(dateText(transaction.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(formatInr(transaction.amountPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AnalyticsScreen(vm: ExpenseViewModel) {
    val analytics by vm.analytics.collectAsState()
    val selected by vm.selectedRange
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TabRow(
                selectedTabIndex = listOf("Today", "Week", "Month").indexOf(selected).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("Today", "Week", "Month").forEach { label ->
                    Tab(selected = selected == label, onClick = { vm.setRange(label) }, text = { Text(label) })
                }
            }
        }
        item { SectionHeader("Category Breakdown") }
        item { CategoryPie(analytics.categories) }
        item { SectionHeader("Spending by Category") }
        item { SpendingBars(analytics.categories.map { (it.category?.label ?: "Other") to it.amountPaise }) }
        item { SectionHeader("Spending Trend") }
        item { SimpleTrend(analytics.totalPaise) }
        item { SectionHeader("Top Merchants") }
        items(analytics.merchants) { merchant ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(merchant.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatInr(merchant.amountPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { SectionHeader("Top Categories") }
        items(analytics.categories) { cat ->
            val accent = categoryColor(cat.category)
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                        Spacer(Modifier.width(10.dp))
                        Text(cat.category?.label ?: "Other")
                    }
                    Text(formatInr(cat.amountPaise), fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }
    }
}

@Composable
fun CategoryPie(items: List<AmountByCategory>) {
    val total = items.sumOf { it.amountPaise }.coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        var start = -90f
        items.forEachIndexed { index, item ->
            val sweep = 360f * item.amountPaise / total
            drawArc(
                chartColors[index % chartColors.size],
                start,
                sweep,
                useCenter = true,
                size = Size(180.dp.toPx(), 180.dp.toPx()),
                topLeft = Offset((size.width - 180.dp.toPx()) / 2, 0f)
            )
            start += sweep
        }
    }
}

@Composable
fun SpendingBars(items: List<Pair<String, Long>>) {
    val max = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(6).forEachIndexed { index, (label, amount) ->
            val color = chartColors[index % chartColors.size]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(formatInr(amount), fontWeight = FontWeight.Bold, color = color)
            }
            LinearProgressIndicator(
                progress = { amount.toFloat() / max },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun SimpleTrend(total: Long) {
    val trendColor = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val points = listOf(0.15f, 0.35f, 0.25f, 0.55f, 0.45f, 0.7f, 0.6f)
        val step = size.width / (points.size - 1)
        points.zipWithNext().forEachIndexed { index, pair ->
            drawLine(
                color = trendColor,
                start = Offset(step * index, size.height * (1 - pair.first)),
                end = Offset(step * (index + 1), size.height * (1 - pair.second)),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }
    Text("Current selected range: ${formatInr(total)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun BudgetScreen(vm: ExpenseViewModel) {
    var view by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Budgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        TabRow(
            selectedTabIndex = view,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = view == 0, onClick = { view = 0 }, text = { Text("Manage") })
            Tab(selected = view == 1, onClick = { view = 1 }, text = { Text("Summary") })
        }
        Spacer(Modifier.height(14.dp))
        when (view) {
            0 -> BudgetManageView(vm)
            else -> BudgetSummaryView(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManageView(vm: ExpenseViewModel) {
    val budgets by vm.budgets.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    var category by remember { mutableStateOf(Category.FoodDining) }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set Monthly Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Budgets renew automatically on the 1st of each month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = category.label,
                            onValueChange = {},
                            singleLine = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Category.entries.forEach {
                                DropdownMenuItem(text = { Text(it.label) }, onClick = { category = it; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input -> amount = input.filter { it.isDigit() } },
                        label = { Text("Monthly limit (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            vm.saveBudget(category, amount, budgets.firstOrNull { it.category == category })
                            amount = ""
                        },
                        enabled = amount.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Budget")
                    }
                }
            }
        }
        if (budgets.isNotEmpty()) {
            item { SectionHeader("Your Budgets") }
        }
        items(budgets) { budget ->
            val spent = dashboard.categoryTotals.firstOrNull { it.category == budget.category }?.amountPaise ?: 0L
            val limit = budget.limitPaise.coerceAtLeast(1)
            val percent = (spent * 100f / limit).toInt()
            val ratio = (spent.toFloat() / limit).coerceIn(0f, 1f)
            val overBudget = spent > budget.limitPaise
            val accent = categoryColor(budget.category)
            val barColor = if (overBudget) MaterialTheme.colorScheme.error else accent
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(budget.category.label, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.weight(1f))
                        Text(
                            "$percent% used",
                            fontWeight = FontWeight.Bold,
                            color = barColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = { vm.deleteBudget(budget) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete budget",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Spent ${formatInr(spent)} • Remaining ${formatInr((budget.limitPaise - spent).coerceAtLeast(0))}",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = barColor,
                        trackColor = barColor.copy(alpha = 0.2f)
                    )
                    Text(
                        if (overBudget) "Over budget! Alerts sent at 50%, 75%, 90%, and 100%."
                        else "You'll be alerted at 50%, 75%, 90%, and 100%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSummaryView(vm: ExpenseViewModel) {
    val history by vm.budgetHistory.collectAsState()
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No budget history yet.\nA summary is saved automatically on the 1st of each month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val years = history.map { it.yearMonth.substringBefore("-") }.distinct().sortedDescending()
    var selectedYear by remember(years) { mutableStateOf(years.first()) }
    var yearExpanded by remember { mutableStateOf(false) }
    val monthsForYear = history
        .filter { it.yearMonth.startsWith("$selectedYear-") }
        .groupBy { it.yearMonth }
        .toList()
        .sortedByDescending { it.first }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ExposedDropdownMenuBox(expanded = yearExpanded, onExpandedChange = { yearExpanded = it }) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedYear,
                    onValueChange = {},
                    singleLine = true,
                    label = { Text("Year") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                    years.forEach { y ->
                        DropdownMenuItem(text = { Text(y) }, onClick = { selectedYear = y; yearExpanded = false })
                    }
                }
            }
        }
        items(monthsForYear) { (yearMonth, entries) ->
            val totalLimit = entries.sumOf { it.limitPaise }
            val totalSpent = entries.sumOf { it.spentPaise }
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(monthYearLabel(yearMonth), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Total spent ${formatInr(totalSpent)} of ${formatInr(totalLimit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entries.sortedByDescending { it.spentPaise }.forEach { entry ->
                        val pct = if (entry.limitPaise > 0) (entry.spentPaise * 100 / entry.limitPaise).toInt() else 0
                        val over = entry.spentPaise > entry.limitPaise
                        val accent = categoryColor(entry.category)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                                Spacer(Modifier.width(10.dp))
                                Text(entry.category.label, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${formatInr(entry.spentPaise)} / ${formatInr(entry.limitPaise)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "$pct%${if (over) " • over" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (over) MaterialTheme.colorScheme.error else accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun monthYearLabel(yearMonth: String): String = runCatching {
    val ym = YearMonth.parse(yearMonth)
    "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
}.getOrDefault(yearMonth)

@Composable
fun SettingsScreen(vm: ExpenseViewModel) {
    val context = LocalContext.current
    val darkTheme by vm.darkTheme
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationAccess = remember(refreshKey) { PermissionHelper.hasNotificationListenerAccess(context) }
    val appNotifications = remember(refreshKey) { PermissionHelper.areNotificationsEnabled(context) }
    val smsAccess = remember(refreshKey) { PermissionHelper.hasSmsPermission(context) }
    val batteryOk = remember(refreshKey) { PermissionHelper.isIgnoringBatteryOptimizations(context) }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refreshKey++
        if (result.values.none { it }) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (darkTheme) "Dark theme enabled" else "Light theme enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = vm::setDarkTheme
                    )
                }
            }
        }
        item { SectionHeader("Detection Reliability") }
        item {
            Text(
                "All permissions below must be enabled for expense detection to work when the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            PermissionStatusCard(
                title = "Payment Notification Access",
                description = "Detects UPI and bank payment alerts from other apps",
                enabled = notificationAccess,
                actionLabel = "Enable",
                onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
        }
        item {
            PermissionStatusCard(
                title = "App Notifications",
                description = "Required to alert you when an expense is detected",
                enabled = appNotifications,
                actionLabel = "Enable",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    } else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                }
            )
        }
        item {
            PermissionStatusCard(
                title = "SMS Access",
                description = "Detects bank debit SMS when payment apps don't notify",
                enabled = smsAccess,
                actionLabel = "Enable",
                onAction = { smsLauncher.launch(PermissionHelper.smsPermissions) }
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    val shown = ExpenseNotifications(context).showTest()
                    Toast.makeText(
                        context,
                        if (shown) "Test notification sent" else "Enable notifications first",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Send test notification") }
        }
        item {
            PermissionStatusCard(
                title = "Battery Optimization",
                description = "Prevents the system from stopping background detection",
                enabled = batteryOk,
                actionLabel = "Disable",
                onAction = { context.startActivity(PermissionHelper.batteryOptimizationIntent(context)) }
            )
        }
        item { SectionHeader("Privacy") }
        item {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy-first architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "All transaction processing, merchant learning, analytics, budgets, and reports run locally on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val statusColor = if (enabled) Color(0xFF059669) else Color(0xFFDC2626)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    if (enabled) "Enabled" else "Disabled",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!enabled) {
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    transaction: TransactionEntity,
    onSave: (Category, Priority, String) -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember(transaction.id) { mutableStateOf(transaction.category ?: Category.Other) }
    var priority by remember(transaction.id) { mutableStateOf(transaction.priority ?: Priority.Optional) }
    var notes by remember(transaction.id) { mutableStateOf(transaction.notes) }
    var categoryOpen by remember { mutableStateOf(false) }
    var priorityOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Amount: ${formatInr(transaction.amountPaise)}")
                Text("Merchant: ${transaction.merchant}")
                Text("Date: ${dateText(transaction.timestamp)}")
                Text("Source: ${transaction.source}")
                ExposedDropdownMenuBox(expanded = categoryOpen, onExpandedChange = { categoryOpen = it }) {
                    OutlinedTextField(readOnly = true, value = category.label, onValueChange = {}, label = { Text("Category") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                        Category.entries.forEach { DropdownMenuItem(text = { Text(it.label) }, onClick = { category = it; categoryOpen = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = priorityOpen, onExpandedChange = { priorityOpen = it }) {
                    OutlinedTextField(readOnly = true, value = priority.name, onValueChange = {}, label = { Text("Priority") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = priorityOpen, onDismissRequest = { priorityOpen = false }) {
                        Priority.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { priority = it; priorityOpen = false }) }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Transaction", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(category, priority, notes) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("This removes ${formatInr(transaction.amountPaise)} at ${transaction.merchant} from your history, spending totals, and budgets. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

fun dateText(timestamp: Long): String =
    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))

fun syncTimeText(millis: Long): String {
    if (millis <= 0L) return "Never"
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else -> SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(millis))
    }
}
