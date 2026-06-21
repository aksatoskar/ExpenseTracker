package com.example.expensetracker.domain.usecase.dashboard

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.DashboardState
import com.example.expensetracker.domain.model.SpendingTotals
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Streams the live [DashboardState] (totals, category/priority breakdowns, pending count). */
class ObserveDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<DashboardState> {
        val totals = combine(
            transactionRepository.observeTotal(DateRange.today()),
            transactionRepository.observeTotal(DateRange.week()),
            transactionRepository.observeTotal(DateRange.month()),
            transactionRepository.observeTotal(DateRange.lastWeek())
        ) { today, week, month, lastWeek -> SpendingTotals(today, week, month, lastWeek) }

        return combine(
            totals,
            transactionRepository.observeCategoryTotals(DateRange.month()),
            transactionRepository.observePriorityTotals(DateRange.month()),
            transactionRepository.pendingCount
        ) { t, categoryTotals, priorityTotals, pendingCount ->
            DashboardState(
                todayPaise = t.today,
                weekPaise = t.week,
                monthPaise = t.month,
                lastWeekPaise = t.lastWeek,
                categoryTotals = categoryTotals,
                priorityTotals = priorityTotals,
                pendingCount = pendingCount
            )
        }
    }
}
