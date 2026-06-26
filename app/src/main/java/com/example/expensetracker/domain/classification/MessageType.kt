package com.example.expensetracker.domain.classification

/**
 * Payment-message categories for a debit-tracking app.
 * Only [ActualDebit] with sufficient confidence should trigger ingestion and notification.
 */
enum class MessageType {
    ActualDebit,
    FutureDebit,
    Credit,
    Receipt,
    Otp,
    RewardCashback,
    PhishingSpam,
    Unknown
}

data class MessageClassificationInput(
    val rawText: String,
    val source: String,
    val receivedAtMillis: Long,
    val sender: String? = null,
    val notificationPackage: String? = null
)

data class MessageClassificationResult(
    val type: MessageType,
    /** Confidence score from 0–100. */
    val confidence: Int,
    val reason: String? = null
) {
    val shouldNotify: Boolean
        get() = type == MessageType.ActualDebit && confidence >= NOTIFY_THRESHOLD

    companion object {
        const val NOTIFY_THRESHOLD = 80
    }
}
