package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Sends a "today you spent X" digest summarizing all of today's debits, reviewed or pending. */
class SendDailyDigestUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke() {
        if (!notifier.areNotificationsEnabled()) return

        val transactions = transactionRepository.getAllDebitTransactions(DateRange.today())
        val pendingCount = transactionRepository.getPending().size
        if (transactions.isEmpty()) return

        val total = transactions.sumOf { it.amountPaise }
        val categoryLines = transactions
            .groupBy { it.category ?: Category.Other }
            .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString("\n") { "${it.key.label}: ${formatInr(it.value)}" }

        val pendingLine = when {
            pendingCount == 0 -> "All caught up — no pending reviews."
            pendingCount == 1 -> "1 transaction still needs review."
            else -> "$pendingCount transactions still need review."
        }

        notifier.showReport(
            "Today you spent ${formatInr(total)}",
            "$categoryLines\n$pendingLine"
        )
    }
}
