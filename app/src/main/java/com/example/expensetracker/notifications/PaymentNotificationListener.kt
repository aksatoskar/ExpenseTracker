package com.example.expensetracker.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.parser.NotificationTextExtractor
import com.example.expensetracker.domain.parser.TransactionParser
import com.example.expensetracker.domain.usecase.detection.RecordDetectedMessageUseCase
import com.example.expensetracker.domain.usecase.transaction.IngestTransactionUseCase
import com.example.expensetracker.sync.IngestWorker
import com.example.expensetracker.sync.PendingNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Listens to payment notifications, records parsed debits and ingests them. */
@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {

    @Inject lateinit var ingestTransaction: IngestTransactionUseCase
    @Inject lateinit var parser: TransactionParser
    @Inject lateinit var crashReporter: CrashReporter
    @Inject lateinit var recordDetectedMessage: RecordDetectedMessageUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        PendingNotificationWorker.enqueue(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val text = NotificationTextExtractor.extract(sbn.notification.extras)
        if (text.isBlank()) return

        val parsed = parser.parse(text, "Notification", sbn.postTime) ?: return
        val appContext = applicationContext
        scope.launch {
            try {
                recordDetectedMessage(parsed, sbn.packageName)
                ingestTransaction(parsed)
            } catch (e: Exception) {
                crashReporter.recordNonFatal(e)
                IngestWorker.enqueue(appContext, parsed)
            }
        }
    }
}
