package com.example.expensetracker.domain.model

/** Aggregated spend totals over the standard time windows. */
data class SpendingTotals(
    val today: Long = 0,
    val week: Long = 0,
    val month: Long = 0,
    val lastWeek: Long = 0
)

/** Everything the dashboard needs in a single immutable snapshot. */
data class DashboardState(
    val todayPaise: Long = 0,
    val weekPaise: Long = 0,
    val monthPaise: Long = 0,
    val lastWeekPaise: Long = 0,
    val categoryTotals: List<AmountByCategory> = emptyList(),
    val priorityTotals: List<AmountByPriority> = emptyList(),
    val pendingCount: Int = 0
)

/** Aggregates backing the analytics screen for the currently selected range. */
data class AnalyticsState(
    val categories: List<AmountByCategory> = emptyList(),
    val merchants: List<AmountByMerchant> = emptyList(),
    val totalPaise: Long = 0
)
