package com.example.expensetracker.data.sync

import android.content.Context
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import com.example.expensetracker.sync.CloudSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidCloudSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : CloudSyncScheduler {

    override fun schedule() {
        CloudSyncWorker.enqueue(context)
    }
}
