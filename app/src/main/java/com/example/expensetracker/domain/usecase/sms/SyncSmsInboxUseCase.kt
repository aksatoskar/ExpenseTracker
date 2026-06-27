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
 * Incremental sync scans since the last sync. Manual **Sync now** rescans from the install
 * baseline so previously skipped messages (e.g. deduped in error) can be recovered; dedupe
 * prevents double-inserting transactions already in the database.
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

    suspend operator fun invoke(rescanFromBaseline: Boolean = false): Int = withContext(dispatchers.io) {
        if (!smsRepository.canReadSms()) return@withContext PERMISSION_DENIED

        val now = System.currentTimeMillis()
        val lastSync = settingsRepository.lastSmsSync.first()
        val baseline = settingsRepository.ensureSmsSyncBaseline(now)
        val since = if (rescanFromBaseline) baseline else maxOf(lastSync, baseline)

        var newCount = 0
        smsRepository.readSince(sinceMillis = since, inclusive = rescanFromBaseline).forEach { sms ->
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
