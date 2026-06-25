package com.example.expensetracker.domain.usecase.analytics

import com.example.expensetracker.core.analytics.SpendingTrend
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.AnalyticsState
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Streams the [AnalyticsState] (category totals, top merchants, trend, grand total) for a [DateRange]. */
class ObserveAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(range: DateRange, rangeLabel: String): Flow<AnalyticsState> = combine(
        transactionRepository.observeCategoryTotals(range),
        transactionRepository.observeTopMerchants(range),
        transactionRepository.observeTotal(range),
        transactionRepository.all
    ) { categories, merchants, total, allTransactions ->
        val debitsInRange = allTransactions.filter { transaction ->
            transaction.type == TransactionType.Debit &&
                transaction.status == TransactionStatus.Reviewed &&
                transaction.timestamp in range.startMillis..range.endMillis
        }
        val bucketCount = SpendingTrend.bucketCount(rangeLabel)
        AnalyticsState(
            categories = categories,
            merchants = merchants,
            totalPaise = total,
            trendPoints = SpendingTrend.compute(debitsInRange, range, bucketCount),
            trendLabels = SpendingTrend.labels(rangeLabel, range, bucketCount)
        )
    }
}
