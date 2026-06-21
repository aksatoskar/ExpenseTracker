package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject

/**
 * Persists a freshly [ParsedTransaction], de-duplicating against recent same-amount/merchant rows,
 * auto-applying any learned merchant rule, surfacing a detection notification and re-checking
 * budgets. Returns `true` when a new transaction was stored.
 */
class IngestTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val showDetectedNotification: ShowDetectedNotificationUseCase,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase
) {
    suspend operator fun invoke(parsed: ParsedTransaction): Boolean {
        val key = normalizeMerchant(parsed.merchant)
        val duplicate = transactionRepository.findRecentAmountMatches(
            amountPaise = parsed.amountPaise,
            type = parsed.type,
            start = parsed.timestamp - DUPLICATE_WINDOW_MILLIS,
            end = parsed.timestamp + DUPLICATE_WINDOW_MILLIS
        ).firstOrNull { normalizeMerchant(it.merchant) == key }

        if (duplicate != null) {
            showDetectedNotification(duplicate)
            return false
        }

        val rule = transactionRepository.getMerchantRule(key)
        val transaction = TransactionEntity(
            amountPaise = parsed.amountPaise,
            merchant = parsed.merchant,
            type = parsed.type,
            timestamp = parsed.timestamp,
            source = parsed.source,
            rawText = parsed.rawText,
            category = rule?.category,
            priority = rule?.priority,
            status = TransactionStatus.PendingReview,
            notified = false
        )
        val id = transactionRepository.insert(transaction)
        showDetectedNotification(transaction.copy(id = id))
        transaction.category?.let { checkBudgetAlerts(it) }
        return true
    }

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
