package com.example.expensetracker.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules the 9 PM daily digest and pending-review reminder workers. */
object DailyReminderScheduler {

    const val DIGEST_WORK_NAME = "daily-expense-digest"

    /**
     * Ensures periodic 9 PM reminders are registered. Uses [ExistingPeriodicWorkPolicy.KEEP] so
     * opening the app does not reset the countdown to the next run.
     */
    fun schedule(context: Context, replaceExisting: Boolean = false) {
        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        val ninePmDelay = Duration.between(LocalDateTime.now(), nextNinePm()).toMillis().coerceAtLeast(0)

        val digest = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(ninePmDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DIGEST_WORK_NAME,
            policy,
            digest
        )

        val pendingReviewReminder = PeriodicWorkRequestBuilder<PendingReviewReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(ninePmDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PendingReviewReminderWorker.WORK_NAME,
            policy,
            pendingReviewReminder
        )
    }

    private fun nextNinePm(): LocalDateTime {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(21, 0)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }
}
