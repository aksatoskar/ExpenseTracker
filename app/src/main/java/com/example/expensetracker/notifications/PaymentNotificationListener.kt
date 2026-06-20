package com.example.expensetracker.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.domain.NotificationTextExtractor
import com.example.expensetracker.domain.TransactionParser
import com.example.expensetracker.sync.IngestWorker
import com.example.expensetracker.sync.PendingNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = TransactionParser()

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
                (appContext as ExpenseTrackerApp).repository.ingest(parsed)
            } catch (e: Exception) {
                IngestWorker.enqueue(appContext, parsed)
            }
        }
    }
}
