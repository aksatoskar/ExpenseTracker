package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Sends a "today you spent X" digest summarizing the day's reviewed spend by category. */
class SendDailyDigestUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke() {
        val transactions = transactionRepository.getDebitTransactions(DateRange.today())
        if (transactions.isEmpty()) return

        val total = transactions.sumOf { it.amountPaise }
        val categoryLines = transactions
            .groupBy { it.category ?: Category.Other }
            .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString("\n") { "${it.key.label}: ${formatInr(it.value)}" }
        val pending = transactions.count { it.status == TransactionStatus.PendingReview }

        notifier.showReport(
            "Today you spent ${formatInr(total)}",
            "$categoryLines\nPending reviews: $pending transactions"
        )
    }
}
