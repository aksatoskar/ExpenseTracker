package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject

/**
 * Permanently removes a transaction, cancels any related notification and re-checks the affected
 * budget so totals and alerts stay consistent everywhere.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase
) {
    suspend operator fun invoke(id: Long) {
        val existing = transactionRepository.getTransaction(id)
        transactionRepository.deleteById(id)
        notifier.cancel(id)
        existing?.category?.let { checkBudgetAlerts(it) }
    }
}
