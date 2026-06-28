package com.example.expensetracker.presentation.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.presentation.common.CategoryDropdownField
import com.example.expensetracker.presentation.common.SectionHeader
import com.example.expensetracker.presentation.common.TransactionRow
import com.example.expensetracker.presentation.common.categoryColor
import com.example.expensetracker.presentation.common.categoryIcon
import com.example.expensetracker.presentation.common.monthYearLabel

/** Budget tab hosting the Manage and Summary sub-views. */
@Composable
fun BudgetScreen(openBudget: (Category) -> Unit) {
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
            0 -> BudgetManageView(openBudget)
            else -> BudgetSummaryView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetManageView(onBudgetClick: (Category) -> Unit) {
    val vm: BudgetViewModel = hiltViewModel()
    val budgets by vm.budgets.collectAsState()
    val categoryTotals by vm.monthCategoryTotals.collectAsState()
    var categorySelection by remember {
        mutableStateOf<CategorySelection>(CategorySelection.BuiltIn(Category.FoodDining))
    }
    var amount by remember { mutableStateOf("") }
    val selectedBuiltIn = categorySelection as? CategorySelection.BuiltIn
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
                    CategoryDropdownField(
                        selected = categorySelection,
                        onSelected = { categorySelection = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedBuiltIn == null) {
                        Text(
                            "Monthly budgets apply to built-in categories only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            val builtIn = selectedBuiltIn ?: return@Button
                            vm.saveBudget(builtIn.category, amount, budgets.firstOrNull { it.category == builtIn.category })
                            amount = ""
                        },
                        enabled = amount.isNotBlank() && selectedBuiltIn != null,
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
            val spent = categoryTotals.firstOrNull { it.category == budget.category }?.amountPaise ?: 0L
            val limit = budget.limitPaise.coerceAtLeast(1)
            val percent = (spent * 100f / limit).toInt()
            val ratio = (spent.toFloat() / limit).coerceIn(0f, 1f)
            val overBudget = spent > budget.limitPaise
            val accent = categoryColor(budget.category)
            val barColor = if (overBudget) MaterialTheme.colorScheme.error else accent
            Card(
                Modifier.fillMaxWidth().clickable { onBudgetClick(budget.category) },
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(budget.category.label, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.weight(1f))
                        Text("$percent% used", fontWeight = FontWeight.Bold, color = barColor, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { vm.deleteBudget(budget) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete budget", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Full-screen overlay listing this month's transactions for a single budget category. */
@Composable
fun BudgetDetailScreen(category: Category, onBack: () -> Unit, openReview: (Long) -> Unit) {
    BackHandler(onBack = onBack)
    val vm: BudgetViewModel = hiltViewModel()
    val all by vm.allTransactions.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val range = remember { DateRange.month() }
    val transactions = remember(all, category, range) {
        all.filter {
            it.type == TransactionType.Debit &&
                it.category == category &&
                it.timestamp in range.startMillis..range.endMillis
        }
    }
    val spent = remember(transactions) { transactions.sumOf { it.amountPaise } }
    val budget = budgets.firstOrNull { it.category == category }
    val accent = categoryColor(category)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(category.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f))))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(categoryIcon(category), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Spent this month", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(formatInr(spent), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        budget?.let { b ->
                            val pct = (spent * 100 / b.limitPaise.coerceAtLeast(1)).toInt()
                            Spacer(Modifier.height(6.dp))
                            Text("$pct% of ${formatInr(b.limitPaise)} budget", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text(
                    "${transactions.size} ${if (transactions.size == 1) "transaction" else "transactions"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No transactions in this budget yet this month.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(transactions) { TransactionRow(it, onClick = { openReview(it.id) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSummaryView() {
    val vm: BudgetViewModel = hiltViewModel()
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                                Spacer(Modifier.width(10.dp))
                                Text(entry.category.label, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${formatInr(entry.spentPaise)} / ${formatInr(entry.limitPaise)}", style = MaterialTheme.typography.labelMedium)
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
