package com.example.expensetracker.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.example.expensetracker.MainActivity
import com.example.expensetracker.receivers.DailyReminderAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules the 9 PM daily digest and pending-review reminder using [AlarmManager.setAlarmClock].
 *
 * Alarm-clock alarms are treated as user-visible and continue to fire under Doze, app standby,
 * and battery optimization — unlike [WorkManager] periodic jobs which OEMs often defer for hours.
 */
object DailyReminderScheduler {

    const val ACTION_DAILY_REMINDER = "com.example.expensetracker.DAILY_REMINDER_ALARM"
    const val DIGEST_WORK_NAME = "daily-expense-digest"
    private const val ALARM_REQUEST_CODE = 90_001

    /** @param replaceExisting unused; kept for call-site compatibility with boot/update handlers. */
    fun schedule(context: Context, replaceExisting: Boolean = false) {
        cancelLegacyPeriodicWork(context)

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextNinePmMillis()
        val alarmIntent = alarmPendingIntent(context)
        val showIntent = PendingIntent.getActivity(
            context,
            ALARM_REQUEST_CODE + 1,
            MainActivity.intent(context),
            pendingIntentFlags()
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            alarmIntent
        )
    }

    private fun cancelLegacyPeriodicWork(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DIGEST_WORK_NAME)
            cancelUniqueWork(PendingReviewReminderWorker.WORK_NAME)
        }
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderAlarmReceiver::class.java)
            .setAction(ACTION_DAILY_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags()
        )
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun nextNinePmMillis(): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val todayNinePm = now.toLocalDate().atTime(21, 0)
        val next = if (now.isBefore(todayNinePm)) todayNinePm else todayNinePm.plusDays(1)
        return next.atZone(zone).toInstant().toEpochMilli()
    }
}
