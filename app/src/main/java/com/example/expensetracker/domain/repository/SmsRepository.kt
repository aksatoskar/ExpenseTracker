package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.model.RawSms

/** Read access to the device SMS inbox for transaction recovery. */
interface SmsRepository {
    /** Whether the app currently holds the runtime permission required to read SMS. */
    fun canReadSms(): Boolean

    /** Returns inbox messages after [sinceMillis]; [inclusive] includes messages at exactly [sinceMillis]. */
    fun readSince(sinceMillis: Long, inclusive: Boolean = false): List<RawSms>

    /** Returns inbox messages with [startMillis] <= date <= [endMillis], oldest first. */
    fun readBetween(startMillis: Long, endMillis: Long): List<RawSms>
}
