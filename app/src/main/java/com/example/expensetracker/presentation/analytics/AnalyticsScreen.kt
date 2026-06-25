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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun AnalyticsScreen(
    navToken: Int = 0,
    initialRange: String = "Month"
) {
    val vm: AnalyticsViewModel = hiltViewModel()
    val analytics by vm.analytics.collectAsState()
    val selected by vm.selectedRange.collectAsState()

    LaunchedEffect(navToken) {
        if (navToken > 0) vm.setRange(initialRange)
    }

    val hasData = analytics.totalPaise > 0

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(
            selectedTabIndex = listOf("Today", "Week", "Month").indexOf(selected).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            listOf("Today", "Week", "Month").forEach { label ->
                Tab(selected = selected == label, onClick = { vm.setRange(label) }, text = { Text(label) })
            }
        }

        if (!hasData) {
            AnalyticsEmptyState(rangeLabel = selected, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item { SectionHeader("Category Breakdown") }
                item { CategoryPie(analytics.categories) }
                item { SectionHeader("Spending by Category") }
                item { SpendingBars(analytics.categories.map { (it.category?.label ?: "Other") to it.amountPaise }) }
                item { SectionHeader("Spending Trend") }
                item { SpendingTrendChart(analytics.trendPoints, analytics.trendLabels, analytics.totalPaise) }
                item { SectionHeader("Top Merchants") }
                items(analytics.merchants) { merchant ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                merchant.merchant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatInr(merchant.amountPaise),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item { SectionHeader("Top Categories") }
                items(analytics.categories) { cat ->
                    val accent = categoryColor(cat.category)
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
    }
}

@Composable
private fun AnalyticsEmptyState(rangeLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    "Nothing to chart yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    emptyRangeMessage(rangeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Review or add transactions and your breakdowns will show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun emptyRangeMessage(rangeLabel: String): String = when (rangeLabel) {
    "Today" -> "No reviewed spending recorded for today."
    "Week" -> "No reviewed spending recorded this week."
    else -> "No reviewed spending recorded this month."
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
private fun SpendingTrendChart(points: List<Long>, labels: List<String>, totalPaise: Long) {
    val trendColor = MaterialTheme.colorScheme.secondary
    val max = points.maxOrNull()?.coerceAtLeast(1) ?: 1L
    val normalized = points.map { amount -> amount.toFloat() / max }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            if (normalized.size == 1) {
                val y = size.height * (1 - normalized.first())
                drawLine(
                    color = trendColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            } else {
                val step = size.width / (normalized.size - 1)
                normalized.zipWithNext().forEachIndexed { index, pair ->
                    drawLine(
                        color = trendColor,
                        start = Offset(step * index, size.height * (1 - pair.first)),
                        end = Offset(step * (index + 1), size.height * (1 - pair.second)),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        if (labels.size == points.size) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            "Total: ${formatInr(totalPaise)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
