package com.example.expensetracker.domain.sync

/** Queues a background cloud sync without blocking the caller's thread. */
interface CloudSyncScheduler {
    fun schedule()
}
