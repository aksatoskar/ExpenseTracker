package com.example.expensetracker.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.presentation.common.ChartColors
import com.example.expensetracker.presentation.common.SectionHeader
import com.example.expensetracker.presentation.common.categoryColor

/** Charts tab: category pie, spending bars, a trend sparkline and top merchants/categories. */
@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = hiltViewModel()
    val analytics by vm.analytics.collectAsState()
    val selected by vm.selectedRange.collectAsState()
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
private fun CategoryPie(items: List<AmountByCategory>) {
    val total = items.sumOf { it.amountPaise }.coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        var start = -90f
        items.forEachIndexed { index, item ->
            val sweep = 360f * item.amountPaise / total
            drawArc(
                ChartColors[index % ChartColors.size],
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
private fun SpendingBars(items: List<Pair<String, Long>>) {
    val max = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(6).forEachIndexed { index, (label, amount) ->
            val color = ChartColors[index % ChartColors.size]
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
private fun SimpleTrend(total: Long) {
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
