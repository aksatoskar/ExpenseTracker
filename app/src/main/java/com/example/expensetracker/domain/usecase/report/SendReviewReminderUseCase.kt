package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Reminds the user to review a still-pending transaction; no-op if already handled. */
class SendReviewReminderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke(transactionId: Long) {
        val transaction = transactionRepository.getTransaction(transactionId) ?: return
        if (transaction.status != TransactionStatus.PendingReview) return
        notifier.showReminder(
            transactionId,
            "Review expense",
            "${formatInr(transaction.amountPaise)} spent at ${transaction.merchant}"
        )
    }
}
