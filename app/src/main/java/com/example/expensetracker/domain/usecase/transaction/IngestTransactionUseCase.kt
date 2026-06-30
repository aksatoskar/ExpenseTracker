package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import com.example.expensetracker.core.transaction.TransactionDedupe
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persists a freshly [ParsedTransaction], de-duplicating same spends seen on multiple channels
 * (SMS + notification), auto-applying merchant rules and surfacing one detection notification.
 */
@Singleton
class IngestTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val showDetectedNotification: ShowDetectedNotificationUseCase,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase,
    private val cloudSyncScheduler: CloudSyncScheduler
) {
    private val ingestMutex = Mutex()

    suspend operator fun invoke(parsed: ParsedTransaction, notifyUser: Boolean = true): Boolean = ingestMutex.withLock {
        val recent = transactionRepository.findRecentAmountMatches(
            amountPaise = parsed.amountPaise,
            type = parsed.type,
            start = parsed.timestamp - DUPLICATE_WINDOW_MILLIS,
            end = parsed.timestamp + DUPLICATE_WINDOW_MILLIS
        )
        if (recent.any { it.matches(parsed) }) return false

        val key = normalizeMerchant(parsed.merchant)
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
            notified = !notifyUser
        )
        val id = transactionRepository.insert(transaction)
        if (notifyUser) {
            showDetectedNotification(transaction.copy(id = id))
        }
        transaction.category?.let { checkBudgetAlerts(it) }
        cloudSyncScheduler.schedule()
        true
    }

    private fun TransactionEntity.matches(parsed: ParsedTransaction): Boolean =
        TransactionDedupe.isSameTransaction(
            amountA = parsed.amountPaise,
            rawA = parsed.rawText,
            merchantA = parsed.merchant,
            timestampA = parsed.timestamp,
            amountB = amountPaise,
            rawB = rawText,
            merchantB = merchant,
            timestampB = timestamp,
            windowMillis = DUPLICATE_WINDOW_MILLIS
        )

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = TransactionDedupe.DEFAULT_WINDOW_MILLIS
    }
}
