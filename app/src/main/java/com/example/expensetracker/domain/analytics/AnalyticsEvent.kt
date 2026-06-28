package com.example.expensetracker.domain.analytics

import com.example.expensetracker.domain.model.Category

/**
 * Typed analytics events the app reports.
 *
 * [DebitMessageReceived] is logged once per debit-like SMS/notification (parser gate) with outcome
 * fields so support can trace missed notifications in GA4.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {

    /** A transaction was detected and ingested from SMS or a payment notification. */
    data class TransactionDetected(val source: String) :
        AnalyticsEvent("transaction_detected", mapOf("source" to source))

    /**
     * A debit-style SMS or payment notification was received and processed.
     * Register GA4 event-scoped dimensions: message_preview, sender, merchant_name, outcome,
     * message_type, reject_reason, notification_shown, message_id, source, amount_paise, confidence.
     */
    data class DebitMessageReceived(
        val messageId: String,
        val preview: String,
        val source: String,
        val sender: String,
        val merchant: String,
        val amountPaise: Long,
        val outcome: String,
        val messageType: String,
        val confidence: Int,
        val notificationShown: Boolean,
        val rejectReason: String? = null
    ) : AnalyticsEvent(
        "debit_message_received",
        buildMap {
            put("message_id", messageId)
            put("message_preview", analyticsParamText(preview))
            put("source", analyticsParamText(source, 40))
            put("sender", analyticsParamText(sender, 40))
            put("merchant_name", analyticsParamText(merchant, 40))
            put("amount_paise", amountPaise)
            put("outcome", outcome)
            put("message_type", messageType)
            put("confidence", confidence)
            put("notification_shown", notificationShown)
            rejectReason?.let { put("reject_reason", analyticsParamText(it, 100)) }
        }
    )

    /** The user manually added a transaction via the dashboard FAB. */
    data class ManualTransactionAdded(val category: Category) :
        AnalyticsEvent("manual_transaction_added", mapOf("category" to category.name))

    /** The user confirmed a pending transaction in the review dialog. */
    data class ReviewSaved(val category: Category) :
        AnalyticsEvent("review_saved", mapOf("category" to category.name))

    /** A transaction was deleted by the user. */
    data object TransactionDeleted : AnalyticsEvent("transaction_deleted")

    /** A budget was created or updated for [category]. */
    data class BudgetSet(val category: Category) :
        AnalyticsEvent("budget_set", mapOf("category" to category.name))

    /** A budget was removed for [category]. */
    data class BudgetDeleted(val category: Category) :
        AnalyticsEvent("budget_deleted", mapOf("category" to category.name))

    /** A manual SMS inbox sync completed, recovering [found] new transactions. */
    data class SmsSynced(val found: Int) :
        AnalyticsEvent("sms_synced", mapOf("found" to found))
}
