package com.example.expensetracker.domain.notification

import com.example.expensetracker.domain.model.Category

/**
 * Framework-free abstraction over user-facing notifications, so domain code never touches the
 * Android notification APIs directly. Implemented by the data layer.
 */
interface Notifier {
    /** Registers notification channels; safe to call repeatedly. */
    fun createChannels()

    /** Shows the "expense detected" prompt. Returns `false` if notifications are not permitted. */
    fun showDetected(transactionId: Long, amountPaise: Long, merchant: String, source: String): Boolean

    fun showReminder(transactionId: Long, title: String, body: String): Boolean

    fun showTest(): Boolean

    fun showBudgetAlert(category: Category, thresholdPercent: Int, spentPaise: Long, limitPaise: Long): Boolean

    fun showReport(title: String, body: String): Boolean

    fun cancel(transactionId: Long)

    fun areNotificationsEnabled(): Boolean
}
