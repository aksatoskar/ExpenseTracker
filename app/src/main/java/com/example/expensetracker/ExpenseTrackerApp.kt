package com.example.expensetracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.notifications.ExpenseNotifications
import com.example.expensetracker.sync.DailyDigestWorker
import com.example.expensetracker.sync.MonthlyReportWorker
import com.example.expensetracker.sync.PendingNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ExpenseTrackerApp : Application() {
    lateinit var repository: ExpenseRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ExpenseNotifications(this).createChannels()
        repository = ExpenseRepository(this, ExpenseDatabase.get(this).expenseDao())
        scheduleRecurringWork()
        recoverMissedNotifications()
    }

    private fun recoverMissedNotifications() {
        appScope.launch {
            repository.notifyPendingUnnotified()
        }
        PendingNotificationWorker.enqueue(this)
    }

    private fun scheduleRecurringWork() {
        val digestDelay = Duration.between(LocalDateTime.now(), nextNinePm()).toMillis().coerceAtLeast(0)
        val digest = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(digestDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily-expense-digest",
            ExistingPeriodicWorkPolicy.UPDATE,
            digest
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
