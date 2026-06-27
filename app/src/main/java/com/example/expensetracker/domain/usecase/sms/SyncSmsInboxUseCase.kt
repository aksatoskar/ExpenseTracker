package com.example.expensetracker.domain.usecase.sms

import com.example.expensetracker.core.concurrency.DispatcherProvider
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.repository.SmsRepository
import com.example.expensetracker.domain.usecase.detection.IncomingMessageOutcome
import com.example.expensetracker.domain.usecase.detection.ProcessIncomingMessageUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Recovers debit transactions from the SMS inbox that live broadcasts may have missed.
 *
 * Background startup sync is incremental since the last sync. Manual sync uses an explicit
 * [range] (default **today** from the UI). Dedupe prevents double-inserting transactions
 * already in the database.
 */
class SyncSmsInboxUseCase @Inject constructor(
    private val smsRepository: SmsRepository,
    private val settingsRepository: SettingsRepository,
    private val processIncomingMessage: ProcessIncomingMessageUseCase,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        const val PERMISSION_DENIED = -1
    }

    /** [range] null = incremental since last sync; non-null = scan only that inclusive window. */
    suspend operator fun invoke(range: DateRange? = null): Int = withContext(dispatchers.io) {
        if (!smsRepository.canReadSms()) return@withContext PERMISSION_DENIED

        val now = System.currentTimeMillis()
        val lastSync = settingsRepository.lastSmsSync.first()
        val baseline = settingsRepository.ensureSmsSyncBaseline(now)

        val messages = if (range == null) {
            val since = maxOf(lastSync, baseline)
            smsRepository.readSince(sinceMillis = since, inclusive = false)
        } else {
            val start = maxOf(range.startMillis, baseline)
            val end = minOf(range.endMillis, now)
            if (start > end) return@withContext 0
            smsRepository.readBetween(startMillis = start, endMillis = end)
        }

        var newCount = 0
        messages.forEach { sms ->
            when (
                processIncomingMessage(
                    text = sms.body,
                    source = "SMS",
                    timestamp = sms.timestamp,
                    sender = sms.address
                )
            ) {
                IncomingMessageOutcome.Ingested -> newCount++
                IncomingMessageOutcome.NotTransaction,
                IncomingMessageOutcome.Duplicate,
                is IncomingMessageOutcome.Rejected -> Unit
            }
        }
        settingsRepository.setLastSmsSync(now)
        newCount
    }
}
