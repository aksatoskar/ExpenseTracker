package com.example.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.expensetracker.sync.ReminderWorker
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND_LATER) return
        val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        if (id <= 0) return
        listOf(1L, 6L, 24L).forEach { hours ->
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(hours, TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.KEY_TRANSACTION_ID to id))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    companion object {
        const val ACTION_REMIND_LATER = "com.example.expensetracker.REMIND_LATER"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }
}
