package com.example.expensetracker.domain.usecase.sms

import com.example.expensetracker.core.concurrency.DispatcherProvider
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
 * Scans messages since `max(lastSync, installBaseline)`, ingests new debits and advances the
 * last-sync timestamp. The baseline is recorded on this install's first sync (and reset by a
 * reinstall), so messages predating the current installation are never ingested. Returns the
 * number of new transactions, or `-1` when SMS read permission is missing.
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

    suspend operator fun invoke(): Int = withContext(dispatchers.io) {
        if (!smsRepository.canReadSms()) return@withContext PERMISSION_DENIED

        val now = System.currentTimeMillis()
        val lastSync = settingsRepository.lastSmsSync.first()
        val baseline = settingsRepository.ensureSmsSyncBaseline(now)
        val since = maxOf(lastSync, baseline)

        var newCount = 0
        smsRepository.readSince(since).forEach { sms ->
            when (
                processIncomingMessage(
                    text = sms.body,
                    source = "SMS",
                    timestamp = sms.timestamp
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
