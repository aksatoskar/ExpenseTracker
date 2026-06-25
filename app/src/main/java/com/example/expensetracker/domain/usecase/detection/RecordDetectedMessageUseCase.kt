package com.example.expensetracker.domain.usecase.detection

import com.example.expensetracker.core.transaction.TransactionDedupe
import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import javax.inject.Inject

/** Persists a parsed debit SMS/notification for the user to review in Settings. */
class RecordDetectedMessageUseCase @Inject constructor(
    private val repository: DetectedMessageRepository
) {
    suspend operator fun invoke(parsed: ParsedTransaction, sender: String? = null) {
        val recent = repository.getRecentMessages(parsed.timestamp, DUPLICATE_WINDOW_MILLIS)
        if (recent.any { it.matches(parsed) }) return
        repository.insert(
            DetectedMessageEntity(
                source = parsed.source,
                rawText = parsed.rawText,
                sender = sender,
                timestamp = parsed.timestamp,
                amountPaise = parsed.amountPaise,
                merchant = parsed.merchant
            )
        )
    }

    private fun DetectedMessageEntity.matches(parsed: ParsedTransaction): Boolean =
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
