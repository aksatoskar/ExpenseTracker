package com.example.expensetracker.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.parser.NotificationTextExtractor
import com.example.expensetracker.domain.usecase.detection.ProcessIncomingMessageUseCase
import com.example.expensetracker.sync.PendingNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Listens to payment notifications, classifies parsed debits and ingests only valid ones. */
@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {

    @Inject lateinit var processIncomingMessage: ProcessIncomingMessageUseCase
    @Inject lateinit var crashReporter: CrashReporter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        PendingNotificationWorker.enqueue(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val text = NotificationTextExtractor.extract(sbn.notification.extras)
        if (text.isBlank()) return

        scope.launch {
            try {
                processIncomingMessage(
                    text = text,
                    source = "Notification",
                    timestamp = sbn.postTime,
                    notificationPackage = sbn.packageName
                )
            } catch (e: Exception) {
                crashReporter.recordNonFatal(e)
            }
        }
    }
}
