package com.example.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.sync.DailyDigestWorker
import com.example.expensetracker.sync.DailyReminderScheduler
import com.example.expensetracker.sync.PendingReviewReminderWorker

/**
 * Fires at 9 PM via [android.app.AlarmManager.setAlarmClock], which is exempt from Doze and
 * battery optimization delays. Enqueues one-shot digest/review workers and schedules tomorrow.
 */
class DailyReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            DailyReminderScheduler.ACTION_DAILY_REMINDER -> enqueueDailyReminders(context)
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        DailyReminderScheduler.schedule(context.applicationContext)
    }

    private fun enqueueDailyReminders(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.enqueue(OneTimeWorkRequestBuilder<DailyDigestWorker>().build())
        workManager.enqueue(OneTimeWorkRequestBuilder<PendingReviewReminderWorker>().build())
    }
}
