package com.example.expensetracker.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules the daily 9 PM digest and pending-review reminder via strict periodic WorkManager. */
object DailyReminderScheduler {

    const val DAILY_REMINDER_WORK_NAME = "daily-reminder"
    private const val LEGACY_DIGEST_WORK_NAME = "daily-expense-digest"
    private const val LEGACY_PENDING_REVIEW_WORK_NAME = "daily-pending-review-reminder"

    private val reminderConstraints: Constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(false)
        .setRequiresCharging(false)
        .setRequiresDeviceIdle(false)
        .setRequiresStorageNotLow(false)
        .build()

    fun schedule(context: Context, replaceExisting: Boolean = false) {
        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        val ninePmDelay = millisUntilNextNinePm()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK_NAME,
            policy,
            buildStrictDailyPeriodicWork(ninePmDelay)
        )
        cancelLegacyPeriodicWork(context)
    }

    /**
     * 24 h repeat with a 5-minute flex window (WorkManager minimum). Avoids the default 12-hour
     * flex for 24-hour work, so the job runs near the end of each daily interval (~9 PM).
     */
    private fun buildStrictDailyPeriodicWork(initialDelayMillis: Long): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<DailyReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(reminderConstraints)
            .build()

    private fun cancelLegacyPeriodicWork(context: Context) {
        WorkManager.getInstance(context.applicationContext).apply {
            cancelUniqueWork(LEGACY_DIGEST_WORK_NAME)
            cancelUniqueWork(LEGACY_PENDING_REVIEW_WORK_NAME)
        }
    }

    private fun millisUntilNextNinePm(): Long {
        val now = LocalDateTime.now()
        val todayNinePm = now.toLocalDate().atTime(21, 0)
        val next = if (now.isBefore(todayNinePm)) todayNinePm else todayNinePm.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }
}
