package com.example.expensetracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.notifications.NotificationActionReceiver
import com.example.expensetracker.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [Notifier] built on [NotificationCompat]. Owns channel creation and
 * the mapping from transaction ids to stable notification ids.
 */
@Singleton
class AndroidNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Notifier {

    override fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(DETECTED_CHANNEL, "Expense detection", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when a new expense is detected from SMS or payment apps"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, "Expense reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(REPORT_CHANNEL, "Reports and digests", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(BUDGET_CHANNEL, "Budget alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Warns you when category spending approaches or exceeds your budget"
                enableVibration(true)
            }
        )
    }

    override fun areNotificationsEnabled(): Boolean = PermissionHelper.areNotificationsEnabled(context)

    override fun showDetected(transactionId: Long, amountPaise: Long, merchant: String, source: String): Boolean {
        if (!areNotificationsEnabled()) return false

        val reviewIntent = MainActivity.intent(context, reviewTransactionId = transactionId)
        val remindIntent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(NotificationActionReceiver.ACTION_REMIND_LATER)
            .putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transactionId)

        val notificationId = notificationIdFor(transactionId)
        val notification = NotificationCompat.Builder(context, DETECTED_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Expense Detected")
            .setContentText("${formatInr(amountPaise)} spent at $merchant")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${formatInr(amountPaise)} spent at $merchant via $source")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(PendingIntent.getActivity(context, notificationId, reviewIntent, flags()))
            .addAction(0, "Review Now", PendingIntent.getActivity(context, notificationId + 1, reviewIntent, flags()))
            .addAction(0, "Remind Later", PendingIntent.getBroadcast(context, notificationId + 2, remindIntent, flags()))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    override fun showReminder(transactionId: Long, title: String, body: String): Boolean {
        if (!areNotificationsEnabled()) return false
        val notificationId = notificationIdFor(transactionId) + REMINDER_ID_OFFSET
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(PendingIntent.getActivity(context, notificationId, MainActivity.intent(context, transactionId), flags()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    override fun showTest(): Boolean {
        if (!areNotificationsEnabled()) return false
        val notification = NotificationCompat.Builder(context, DETECTED_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Notifications are working")
            .setContentText("You'll be alerted here whenever a new expense is detected.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(PendingIntent.getActivity(context, 40_000, MainActivity.intent(context), flags()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(40_000, notification)
        return true
    }

    override fun showBudgetAlert(category: Category, thresholdPercent: Int, spentPaise: Long, limitPaise: Long): Boolean {
        if (!areNotificationsEnabled()) return false
        val title = if (thresholdPercent >= 100) {
            "Budget exceeded: ${category.label}"
        } else {
            "$thresholdPercent% of ${category.label} budget used"
        }
        val body = "${formatInr(spentPaise)} of ${formatInr(limitPaise)} spent on ${category.label} this month."
        val notificationId = BUDGET_ALERT_BASE_ID + category.ordinal
        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(PendingIntent.getActivity(context, notificationId, MainActivity.intent(context), flags()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    override fun showReport(title: String, body: String): Boolean {
        if (!areNotificationsEnabled()) return false
        val notification = NotificationCompat.Builder(context, REPORT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(PendingIntent.getActivity(context, 30_000, MainActivity.intent(context), flags()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(30_000, notification)
        return true
    }

    override fun cancel(transactionId: Long) {
        val manager = NotificationManagerCompat.from(context)
        val baseId = notificationIdFor(transactionId)
        manager.cancel(baseId)
        manager.cancel(baseId + REMINDER_ID_OFFSET)
    }

    private fun notificationIdFor(transactionId: Long): Int =
        (transactionId xor (transactionId shr 32)).toInt() and 0x7FFFFFFF

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    companion object {
        const val DETECTED_CHANNEL = "expense_detected"
        const val REMINDER_CHANNEL = "expense_reminders"
        const val REPORT_CHANNEL = "expense_reports"
        const val BUDGET_CHANNEL = "budget_alerts"
        private const val REMINDER_ID_OFFSET = 100_000
        private const val BUDGET_ALERT_BASE_ID = 50_000
    }
}
