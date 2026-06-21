package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.model.RawSms

/** Read access to the device SMS inbox for transaction recovery. */
interface SmsRepository {
    /** Whether the app currently holds the runtime permission required to read SMS. */
    fun canReadSms(): Boolean

    /** Returns inbox messages newer than [sinceMillis]. */
    fun readSince(sinceMillis: Long): List<RawSms>

    /** Epoch millis when the app was first installed, used as the earliest sync boundary. */
    fun appInstallTimeMillis(): Long
}
