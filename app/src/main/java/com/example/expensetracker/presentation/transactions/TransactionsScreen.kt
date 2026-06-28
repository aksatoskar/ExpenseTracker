package com.example.expensetracker.presentation.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.presentation.common.CategoryFilterDropdown
import com.example.expensetracker.presentation.common.FilterDropdown
import com.example.expensetracker.presentation.common.TransactionRow

/** Transactions tab with merchant search, period chips, sort/category dropdowns and a summary. */
@Composable
fun TransactionsScreen(
    openReview: (Long) -> Unit,
    navToken: Int = 0,
    initialPeriod: TxnPeriod = TxnPeriod.All
) {
    val vm: TransactionsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    val customCategoryNames = remember(customCategories) {
        customCategories.associate { it.id to it.name }
    }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (!state.hasMore || state.transactions.isEmpty()) return@derivedStateOf false
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(navToken) {
        if (navToken > 0) vm.applyNavFilters(initialPeriod)
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadNextPage()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = { Text("Search merchant") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TxnPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = state.period == p,
                        onClick = { vm.setPeriod(p) },
                        label = { Text(p.label) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilterDropdown(
                    label = "Sort by",
                    value = state.sort.label,
                    options = TxnSort.entries.map { it.label },
                    onSelect = { vm.setSort(TxnSort.entries[it]) },
                    modifier = Modifier.weight(1f)
                )
                CategoryFilterDropdown(
                    selected = state.categoryFilter,
                    customCategories = customCategories,
                    onSelected = vm::setCategoryFilter,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.totalCount} ${if (state.totalCount == 1) "transaction" else "transactions"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatInr(state.totalPaise),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (state.transactions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No transactions match your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(state.transactions, key = { it.id }) { transaction ->
            TransactionRow(
                transaction,
                onClick = { openReview(transaction.id) },
                customCategoryNames = customCategoryNames
            )
        }
        if (state.hasMore) {
            item(key = "loading") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
