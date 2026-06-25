package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Notifies the user at end of day if any transactions are still waiting for review. */
class SendPendingReviewReminderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke() {
        val pending = transactionRepository.getPending()
        if (pending.isEmpty()) return
        notifier.showPendingReviewReminder(pending.size)
    }
}
