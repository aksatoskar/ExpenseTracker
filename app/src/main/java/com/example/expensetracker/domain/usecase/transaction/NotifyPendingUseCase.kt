package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Re-surfaces detection notifications for any pending transactions that were never notified
 * (e.g. the app was force-stopped). Used on launch, boot and listener (re)connection.
 */
class NotifyPendingUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val showDetectedNotification: ShowDetectedNotificationUseCase
) {
    suspend operator fun invoke() {
        transactionRepository.getPending()
            .filter { !it.notified }
            .forEach { showDetectedNotification(it) }
    }
}
