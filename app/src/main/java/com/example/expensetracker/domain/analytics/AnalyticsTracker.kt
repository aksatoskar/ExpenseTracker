package com.example.expensetracker.domain.analytics

/**
 * Framework-free abstraction for product analytics.
 *
 * The domain and presentation layers depend on this interface; the concrete Firebase
 * implementation lives in the data layer and is wired in via Hilt.
 */
interface AnalyticsTracker {

    /** Logs a screen view for [screenName] (e.g. a bottom-nav destination). */
    fun logScreen(screenName: String)

    /** Logs a typed product [event]. */
    fun log(event: AnalyticsEvent)
}
