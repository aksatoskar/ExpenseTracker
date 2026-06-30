package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.usecase.sync.SyncDataUseCase
import com.example.expensetracker.domain.usecase.sync.SyncOutcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Runs Firestore sync off the main thread with network connectivity and retry on failure. */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncData: SyncDataUseCase,
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val crashReporter: CrashReporter
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!featureFlagsRepository.featureFlags.first().cloudSyncEnabled) {
            return Result.success()
        }
        return try {
            when (syncData()) {
                SyncOutcome.NotSignedIn -> Result.success()
                is SyncOutcome.Success -> Result.success()
                is SyncOutcome.Failed -> Result.retry()
            }
        } catch (e: Exception) {
            crashReporter.recordNonFatal(e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "cloud-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
