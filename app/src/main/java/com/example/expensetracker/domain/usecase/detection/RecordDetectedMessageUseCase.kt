package com.example.expensetracker.domain.usecase.detection

import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import javax.inject.Inject

/** Persists a parsed debit SMS/notification for the user to review in Settings. */
class RecordDetectedMessageUseCase @Inject constructor(
    private val repository: DetectedMessageRepository
) {
    suspend operator fun invoke(parsed: ParsedTransaction, sender: String? = null) {
        if (repository.hasNearDuplicate(parsed.rawText, parsed.timestamp, DUPLICATE_WINDOW_MILLIS)) return
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

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 60_000L
    }
}
