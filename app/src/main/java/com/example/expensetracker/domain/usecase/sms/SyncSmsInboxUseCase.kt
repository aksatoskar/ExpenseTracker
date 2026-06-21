package com.example.expensetracker.domain.usecase.sms

import com.example.expensetracker.core.concurrency.DispatcherProvider
import com.example.expensetracker.domain.parser.TransactionParser
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.repository.SmsRepository
import com.example.expensetracker.domain.usecase.transaction.IngestTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Recovers debit transactions from the SMS inbox that live broadcasts may have missed.
 *
 * Scans messages since `max(lastSync, appInstall)`, ingests new debits and advances the last-sync
 * timestamp. Returns the number of new transactions, or `-1` when SMS read permission is missing.
 */
class SyncSmsInboxUseCase @Inject constructor(
    private val smsRepository: SmsRepository,
    private val settingsRepository: SettingsRepository,
    private val parser: TransactionParser,
    private val ingestTransaction: IngestTransactionUseCase,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        const val PERMISSION_DENIED = -1
    }

    suspend operator fun invoke(): Int = withContext(dispatchers.io) {
        if (!smsRepository.canReadSms()) return@withContext PERMISSION_DENIED

        val lastSync = settingsRepository.lastSmsSync.first()
        val since = maxOf(lastSync, smsRepository.appInstallTimeMillis())
        val now = System.currentTimeMillis()

        var newCount = 0
        smsRepository.readSince(since).forEach { sms ->
            val parsed = parser.parse(sms.body, "SMS", sms.timestamp) ?: return@forEach
            if (ingestTransaction(parsed)) newCount++
        }
        settingsRepository.setLastSmsSync(now)
        newCount
    }
}
