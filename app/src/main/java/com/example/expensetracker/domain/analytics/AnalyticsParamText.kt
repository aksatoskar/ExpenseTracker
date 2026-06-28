package com.example.expensetracker.domain.analytics

/** Truncates and flattens text for Firebase Analytics parameter limits (100 chars). */
fun analyticsParamText(value: String, maxLength: Int = 100): String =
    value.replace('\n', ' ').replace(Regex("\\s+"), " ").trim().take(maxLength)

/** Stable id to correlate [AnalyticsEvent.DebitMessageDetected] with [AnalyticsEvent.DebitMessageHandled]. */
fun debitMessageAnalyticsId(timestamp: Long, rawText: String): String =
    java.util.UUID.nameUUIDFromBytes("$timestamp|${rawText.take(200)}".toByteArray()).toString()
