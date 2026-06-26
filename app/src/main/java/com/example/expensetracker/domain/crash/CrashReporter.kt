package com.example.expensetracker.domain.crash

/**
 * Framework-free abstraction for crash and non-fatal error reporting.
 *
 * Fatal crashes are captured automatically by the underlying SDK; this interface is used to record
 * otherwise-swallowed background failures (SMS ingest, workers) as non-fatals so they remain
 * visible. The concrete Firebase Crashlytics implementation lives in the data layer.
 */
interface CrashReporter {

    /** Records a handled [throwable] as a non-fatal issue. */
    fun recordNonFatal(throwable: Throwable)

    /** Adds a breadcrumb [message] to the current crash/non-fatal context. */
    fun log(message: String)

    /** Sets a custom key/value pair attached to subsequent reports. */
    fun setKey(key: String, value: String)

    /** Associates crash reports with this app installation. */
    fun setUserId(userId: String)
}
