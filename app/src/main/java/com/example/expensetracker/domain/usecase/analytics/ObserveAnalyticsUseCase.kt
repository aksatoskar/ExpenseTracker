package com.example.expensetracker.domain.usecase.analytics

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.AnalyticsState
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Streams the [AnalyticsState] (category totals, top merchants, grand total) for a [DateRange]. */
class ObserveAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(range: DateRange): Flow<AnalyticsState> = combine(
        transactionRepository.observeCategoryTotals(range),
        transactionRepository.observeTopMerchants(range),
        transactionRepository.observeTotal(range)
    ) { categories, merchants, total -> AnalyticsState(categories, merchants, total) }
}
