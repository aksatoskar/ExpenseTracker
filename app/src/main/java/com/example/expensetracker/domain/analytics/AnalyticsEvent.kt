package com.example.expensetracker.domain.analytics

import com.example.expensetracker.domain.model.Category

/**
 * Typed analytics events the app reports.
 *
 * Each event carries only non-sensitive metadata (category, source, counts); raw money amounts and
 * merchant text are deliberately never logged. Implementations map [name] and [params] to the
 * underlying analytics SDK.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {

    /** A transaction was detected and ingested from SMS or a payment notification. */
    data class TransactionDetected(val source: String) :
        AnalyticsEvent("transaction_detected", mapOf("source" to source))

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
