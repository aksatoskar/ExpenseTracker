package com.example.expensetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.usecase.transaction.NotifyPendingUseCase
import com.example.expensetracker.domain.usecase.identity.InitializeAppIdentityUseCase
import com.example.expensetracker.sync.DailyDigestWorker
import com.example.expensetracker.sync.MonthlyReportWorker
import com.example.expensetracker.sync.PendingNotificationWorker
import com.example.expensetracker.sync.PendingReviewReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point. Hilt provides the [HiltWorkerFactory] used to construct injected
 * workers, while [onCreate] sets up notification channels, schedules recurring work and recovers
 * any notifications missed while the app was not running.
 */
@HiltAndroidApp
class ExpenseTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var notifyPending: NotifyPendingUseCase
    @Inject lateinit var initializeAppIdentity: InitializeAppIdentityUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        scheduleRecurringWork()
        recoverMissedNotifications()
        appScope.launch { runCatching { initializeAppIdentity() } }
    }

    private fun recoverMissedNotifications() {
        appScope.launch { notifyPending() }
        PendingNotificationWorker.enqueue(this)
    }

    private fun scheduleRecurringWork() {
        val ninePmDelay = Duration.between(LocalDateTime.now(), nextNinePm()).toMillis().coerceAtLeast(0)

        val digest = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(ninePmDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily-expense-digest",
            ExistingPeriodicWorkPolicy.UPDATE,
            digest
        )

        val pendingReviewReminder = PeriodicWorkRequestBuilder<PendingReviewReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(ninePmDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PendingReviewReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            pendingReviewReminder
        )

        val monthly = PeriodicWorkRequestBuilder<MonthlyReportWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "monthly-report",
            ExistingPeriodicWorkPolicy.UPDATE,
            monthly
        )
    }

    private fun nextNinePm(): LocalDateTime {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(21, 0)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }
}
