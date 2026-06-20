package com.example.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.domain.TransactionParser
import com.example.expensetracker.sync.IngestWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val text = messages.joinToString(" ") { it.messageBody.orEmpty() }
        if (text.isBlank()) return

        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val parsed = TransactionParser().parse(text, "SMS", timestamp) ?: return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        scope.launch {
            try {
                (appContext as ExpenseTrackerApp).repository.ingest(parsed)
            } catch (e: Exception) {
                IngestWorker.enqueue(appContext, parsed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
