package com.example.expensetracker.domain.usecase.detection

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageLabel
import com.example.expensetracker.domain.parser.TransactionParser
import com.example.expensetracker.domain.usecase.transaction.IngestTransactionUseCase
import javax.inject.Inject

/** Parses, classifies and ingests only valid debit SMS / notification bodies. */
class ProcessIncomingMessageUseCase @Inject constructor(
    private val parser: TransactionParser,
    private val classifyMessage: ClassifyTransactionMessageUseCase,
    private val recordDetectedMessage: RecordDetectedMessageUseCase,
    private val ingestTransaction: IngestTransactionUseCase
) {
    suspend operator fun invoke(
        text: String,
        source: String,
        timestamp: Long,
        sender: String? = null,
        notificationPackage: String? = null
    ): IncomingMessageOutcome {
        val parsed = parser.parse(text, source, timestamp) ?: return IncomingMessageOutcome.NotTransaction

        val classification = classifyMessage(
            MessageClassificationInput(
                rawText = parsed.rawText,
                source = source,
                receivedAtMillis = timestamp,
                sender = sender,
                notificationPackage = notificationPackage
            )
        )
        if (classification.label != MessageLabel.ValidDebit) {
            return IncomingMessageOutcome.Rejected(classification)
        }

        recordDetectedMessage(parsed, sender ?: notificationPackage)
        return if (ingestTransaction(parsed)) {
            IncomingMessageOutcome.Ingested
        } else {
            IncomingMessageOutcome.Duplicate
        }
    }
}

sealed interface IncomingMessageOutcome {
    data object NotTransaction : IncomingMessageOutcome
    data object Ingested : IncomingMessageOutcome
    data object Duplicate : IncomingMessageOutcome
    data class Rejected(val classification: MessageClassificationResult) : IncomingMessageOutcome
}
