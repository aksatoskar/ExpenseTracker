package com.example.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.domain.parser.TransactionParser
import com.example.expensetracker.domain.usecase.transaction.IngestTransactionUseCase
import com.example.expensetracker.sync.IngestWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS broadcasts, parses debit messages and ingests them immediately.
 * Falls back to [IngestWorker] if direct ingestion fails, so detection is never lost.
 *
 * Dependencies are pulled from the Hilt singleton graph via [EntryPointAccessors] (the
 * compile-safe injection path for broadcast receivers).
 */
class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun ingestTransactionUseCase(): IngestTransactionUseCase
        fun transactionParser(): TransactionParser
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val text = messages.joinToString(" ") { it.messageBody.orEmpty() }
        if (text.isBlank()) return

        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, SmsReceiverEntryPoint::class.java)

        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val parsed = entryPoint.transactionParser().parse(text, "SMS", timestamp) ?: return
        val ingestTransaction = entryPoint.ingestTransactionUseCase()

        val pendingResult = goAsync()
        scope.launch {
            try {
                ingestTransaction(parsed)
            } catch (e: Exception) {
                IngestWorker.enqueue(appContext, parsed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
