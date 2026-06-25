package com.example.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.sync.ReminderWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/** Handles notification action buttons (e.g. Remind Later) and always dismisses the alert. */
class NotificationActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionEntryPoint {
        fun notifier(): Notifier
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        if (id <= 0) return

        val appContext = context.applicationContext
        val notifier = EntryPointAccessors
            .fromApplication(appContext, NotificationActionEntryPoint::class.java)
            .notifier()
        notifier.cancel(id)

        if (!intent.getBooleanExtra(EXTRA_REMIND_LATER, false)) return

        listOf(1L, 6L, 24L).forEach { hours ->
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(hours, TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.KEY_TRANSACTION_ID to id))
                .build()
            WorkManager.getInstance(appContext).enqueue(request)
        }
    }

    companion object {
        const val ACTION_REMIND_LATER = "com.example.expensetracker.REMIND_LATER"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_REMIND_LATER = "remind_later"
    }
}
