package com.example.expensetracker.domain.classification

/** Outcome of classifying a parsed debit-looking SMS or notification. */
enum class MessageLabel {
    ValidDebit,
    Spam,
    Invalid
}

data class MessageClassificationInput(
    val rawText: String,
    val source: String,
    val receivedAtMillis: Long,
    val sender: String? = null,
    val notificationPackage: String? = null
)

data class MessageClassificationResult(
    val label: MessageLabel,
    val confidence: Float,
    val reason: String? = null
)
