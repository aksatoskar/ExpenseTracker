package com.example.expensetracker.presentation.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.DashboardState
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.presentation.common.PriorityEssentialColor
import com.example.expensetracker.presentation.common.PriorityOptionalColor
import com.example.expensetracker.presentation.common.PriorityWastefulColor
import com.example.expensetracker.presentation.common.SectionHeader
import com.example.expensetracker.presentation.common.TransactionRow
import com.example.expensetracker.presentation.common.categoryColor
import com.example.expensetracker.presentation.common.categoryIcon
import com.example.expensetracker.presentation.common.syncTimeText
import com.example.expensetracker.presentation.settings.CustomSmsSyncDialog
import com.example.expensetracker.presentation.settings.SettingsViewModel
import com.example.expensetracker.presentation.settings.smsSyncResultMessage
import com.example.expensetracker.presentation.transactions.TxnPeriod

/** Home tab: spending hero, priority breakdown, smart insights and recent transactions. */
@Composable
fun DashboardScreen(
    openReview: (Long) -> Unit,
    openPendingReview: () -> Unit,
    openTransactionsTab: (TxnPeriod) -> Unit,
    openChartsTab: () -> Unit
) {
    val vm: DashboardViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()
    val dashboard = state.dashboard

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DashboardHero(dashboard, openTransactionsTab, openChartsTab) }
        item { SyncStatusCard() }
        if (dashboard.pendingCount > 0) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            pendingMessage(dashboard.pendingCount),
                            Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = openPendingReview,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(
                                    "View all",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            TextButton(
                                onClick = { state.pending.firstOrNull()?.let { openReview(it.id) } },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(
                                    "Review now",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Where it goes") }
        item { PriorityBreakdownCard(dashboard.priorityTotals) }
        item { SectionHeader("Smart Insights") }
        item { InsightList(dashboard, state.budgets) }
        item { SectionHeader("Recent Transactions") }
        items(state.latest) { TransactionRow(it, onClick = { openReview(it.id) }) }
        if (state.latest.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { openTransactionsTab(TxnPeriod.All) }) {
                        Text(
                            "View all transactions",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun pendingMessage(count: Int): String =
    when (count) {
        1 -> "You have 1 uncategorized transaction."
        else -> "You have $count uncategorized transactions."
    }

@Composable
private fun SyncStatusCard() {
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = hiltViewModel()
    val lastSync by settingsVm.lastSmsSync.collectAsState()
    val syncing by settingsVm.isSyncing.collectAsState()
    val smsSyncMinDate by settingsVm.smsSyncMinDate.collectAsState()
    var showCustomSync by remember { mutableStateOf(false) }

    if (showCustomSync) {
        CustomSmsSyncDialog(
            minSelectableDate = smsSyncMinDate,
            onDismiss = { showCustomSync = false },
            onConfirm = { from, to ->
                showCustomSync = false
                settingsVm.syncCustom(from, to) { count ->
                    Toast.makeText(context, smsSyncResultMessage(count), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SMS Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Last synced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            " · ${syncTimeText(lastSync)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!syncing) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCustomSync = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Custom range", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            settingsVm.syncToday { count ->
                                Toast.makeText(context, smsSyncResultMessage(count), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sync today", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                Text(
                    "Scanning inbox…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(
    state: DashboardState,
    openTransactionsTab: (TxnPeriod) -> Unit,
    openChartsTab: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
            .clickable(onClick = openChartsTab)
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
                Text("Spent this month", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(8.dp))
            Text(formatInr(state.monthPaise), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStatPill(
                    label = "Today",
                    amountPaise = state.todayPaise,
                    modifier = Modifier.weight(1f),
                    onClick = { openTransactionsTab(TxnPeriod.Today) }
                )
                HeroStatPill(
                    label = "This week",
                    amountPaise = state.weekPaise,
                    modifier = Modifier.weight(1f),
                    onClick = { openTransactionsTab(TxnPeriod.Week) }
                )
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    amountPaise: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(2.dp))
        Text(formatInr(amountPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
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
                Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
                    if (essential > 0) Box(Modifier.weight(essential.toFloat()).fillMaxHeight().background(PriorityEssentialColor))
                    if (optional > 0) Box(Modifier.weight(optional.toFloat()).fillMaxHeight().background(PriorityOptionalColor))
                    if (wasteful > 0) Box(Modifier.weight(wasteful.toFloat()).fillMaxHeight().background(PriorityWastefulColor))
                }
                Spacer(Modifier.height(14.dp))
                PriorityLegendRow("Essential", essential, total, PriorityEssentialColor)
                Spacer(Modifier.height(8.dp))
                PriorityLegendRow("Optional", optional, total, PriorityOptionalColor)
                Spacer(Modifier.height(8.dp))
                PriorityLegendRow("Wasteful", wasteful, total, PriorityWastefulColor)
            }
        }
    }
}

@Composable
private fun PriorityLegendRow(label: String, amountPaise: Long, total: Long, color: Color) {
    val pct = if (total > 0) (amountPaise * 100 / total).toInt() else 0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("$pct%", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
        Text(formatInr(amountPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class Insight(val icon: ImageVector, val title: String, val detail: String, val color: Color)

private fun buildInsights(state: DashboardState, budgets: List<BudgetEntity>): List<Insight> {
    val total = state.monthPaise
    if (total <= 0L) {
        return listOf(
            Insight(Icons.Default.Lightbulb, "Nothing tracked yet", "Your spending insights for this month will appear here.", Color(0xFF6366F1))
        )
    }
    val insights = mutableListOf<Insight>()

    state.categoryTotals
        .filter { it.category != null }
        .maxByOrNull { it.amountPaise }
        ?.let { top ->
            val cat = top.category!!
            val pct = (top.amountPaise * 100 / total).toInt()
            insights.add(Insight(categoryIcon(cat), "Top category", "${cat.label} • ${formatInr(top.amountPaise)} ($pct%)", categoryColor(cat)))
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
                Insight(Icons.Default.Warning, if (over) "Budget exceeded" else "Budget almost used", "${budget.category.label} at $pct% of limit", Color(0xFFEF4444))
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
        insights.add(Insight(Icons.Default.TrendingUp, "Spending this week", formatInr(week), Color(0xFF6366F1)))
    }

    val wasteful = state.priorityTotals.firstOrNull { it.priority == Priority.Wasteful }?.amountPaise ?: 0L
    if (wasteful > 0L) {
        val pct = (wasteful * 100 / total).toInt()
        insights.add(Insight(Icons.Default.MoneyOff, "Wasteful spending", "$pct% of this month • ${formatInr(wasteful)}", PriorityWastefulColor))
    } else {
        insights.add(Insight(Icons.Default.CheckCircle, "No wasteful spending", "Nicely done — every rupee had a purpose.", Color(0xFF059669)))
    }

    return insights.take(4)
}

@Composable
private fun InsightList(state: DashboardState, budgets: List<BudgetEntity>) {
    val insights = buildInsights(state, budgets)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        insights.forEach { insight ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(insight.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(insight.icon, contentDescription = null, tint = insight.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(insight.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
