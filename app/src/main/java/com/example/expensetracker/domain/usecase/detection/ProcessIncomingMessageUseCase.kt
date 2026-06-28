package com.example.expensetracker.domain.usecase.detection

import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.analytics.debitMessageAnalyticsId
import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.parser.TransactionParser
import com.example.expensetracker.domain.usecase.transaction.IngestTransactionUseCase
import javax.inject.Inject

/** Parses, classifies and ingests only valid debit SMS / notification bodies. */
class ProcessIncomingMessageUseCase @Inject constructor(
    private val parser: TransactionParser,
    private val classifyMessage: ClassifyTransactionMessageUseCase,
    private val recordDetectedMessage: RecordDetectedMessageUseCase,
    private val ingestTransaction: IngestTransactionUseCase,
    private val analytics: AnalyticsTracker
) {
    suspend operator fun invoke(
        text: String,
        source: String,
        timestamp: Long,
        sender: String? = null,
        notificationPackage: String? = null,
        notifyUser: Boolean = true
    ): IncomingMessageOutcome {
        val parsed = parser.parse(text, source, timestamp) ?: return IncomingMessageOutcome.NotTransaction

        val origin = sender ?: notificationPackage.orEmpty()
        val classification = classifyMessage(
            MessageClassificationInput(
                rawText = parsed.rawText,
                source = source,
                receivedAtMillis = timestamp,
                sender = sender,
                notificationPackage = notificationPackage
            )
        )

        if (!classification.shouldNotify) {
            logReceived(parsed, source, origin, classification, outcome = "rejected", notificationShown = false)
            return IncomingMessageOutcome.Rejected(classification)
        }

        recordDetectedMessage(parsed, origin.ifBlank { null })
        return if (ingestTransaction(parsed, notifyUser = notifyUser)) {
            logReceived(parsed, source, origin, classification, outcome = "ingested", notificationShown = notifyUser)
            IncomingMessageOutcome.Ingested
        } else {
            logReceived(parsed, source, origin, classification, outcome = "duplicate", notificationShown = false)
            IncomingMessageOutcome.Duplicate
        }
    }

    private fun logReceived(
        parsed: ParsedTransaction,
        source: String,
        sender: String,
        classification: MessageClassificationResult,
        outcome: String,
        notificationShown: Boolean
    ) {
        analytics.log(
            AnalyticsEvent.DebitMessageReceived(
                messageId = debitMessageAnalyticsId(parsed.timestamp, parsed.rawText),
                preview = parsed.rawText,
                source = source,
                sender = sender,
                merchant = parsed.merchant,
                amountPaise = parsed.amountPaise,
                outcome = outcome,
                messageType = classification.type.name,
                confidence = classification.confidence,
                notificationShown = notificationShown,
                rejectReason = classification.reason
            )
        )
    }
}

sealed interface IncomingMessageOutcome {
    data object NotTransaction : IncomingMessageOutcome
    data object Ingested : IncomingMessageOutcome
    data object Duplicate : IncomingMessageOutcome
    data class Rejected(val classification: MessageClassificationResult) : IncomingMessageOutcome
}
