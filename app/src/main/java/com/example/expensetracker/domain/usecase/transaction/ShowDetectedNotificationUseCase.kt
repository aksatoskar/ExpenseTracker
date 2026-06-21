package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Shows the "expense detected" notification for a pending transaction exactly once, marking it
 * notified on success. Shared by ingestion and the missed-notification recovery path.
 */
class ShowDetectedNotificationUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke(transaction: TransactionEntity) {
        if (transaction.notified || transaction.status != TransactionStatus.PendingReview) return
        val shown = notifier.showDetected(
            transactionId = transaction.id,
            amountPaise = transaction.amountPaise,
            merchant = transaction.merchant,
            source = transaction.source
        )
        if (shown) {
            transactionRepository.update(transaction.copy(notified = true))
        }
    }
}
