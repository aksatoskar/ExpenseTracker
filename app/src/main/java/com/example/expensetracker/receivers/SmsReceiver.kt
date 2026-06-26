package com.example.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.usecase.detection.ProcessIncomingMessageUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receives incoming SMS broadcasts, classifies debit messages and ingests only valid ones. */
class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun processIncomingMessage(): ProcessIncomingMessageUseCase
        fun crashReporter(): CrashReporter
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val text = messages.joinToString(" ") { it.messageBody.orEmpty() }
        if (text.isBlank()) return

        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, SmsReceiverEntryPoint::class.java)
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val sender = messages.firstOrNull()?.originatingAddress

        val pendingResult = goAsync()
        scope.launch {
            try {
                entryPoint.processIncomingMessage()(
                    text = text,
                    source = "SMS",
                    timestamp = timestamp,
                    sender = sender
                )
            } catch (e: Exception) {
                entryPoint.crashReporter().recordNonFatal(e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
