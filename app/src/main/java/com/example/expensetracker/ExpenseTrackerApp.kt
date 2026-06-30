package com.example.expensetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.usecase.transaction.NotifyPendingUseCase
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import com.example.expensetracker.domain.usecase.identity.InitializeAppIdentityUseCase
import com.example.expensetracker.sync.DailyReminderScheduler
import com.example.expensetracker.sync.MonthlyReportWorker
import com.example.expensetracker.sync.PendingNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    @Inject lateinit var cloudSyncScheduler: CloudSyncScheduler

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
        appScope.launch {
            runCatching { initializeAppIdentity() }
            cloudSyncScheduler.schedule()
        }
    }

    private fun recoverMissedNotifications() {
        appScope.launch { notifyPending() }
        PendingNotificationWorker.enqueue(this)
    }

    private fun scheduleRecurringWork() {
        DailyReminderScheduler.schedule(this)

        val monthly = PeriodicWorkRequestBuilder<MonthlyReportWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "monthly-report",
            ExistingPeriodicWorkPolicy.KEEP,
            monthly
        )
    }
}
