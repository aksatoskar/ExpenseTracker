package com.example.expensetracker.domain.sync

/** Outcome of a sync pass: how many records were uploaded and downloaded. */
data class SyncResult(val pushed: Int, val pulled: Int)

/**
 * Two-way sync boundary between local storage and the cloud, scoped to a signed-in user.
 * Implemented by the data layer (Firestore); consumed via [com.example.expensetracker.domain.usecase.sync.SyncDataUseCase].
 */
interface SyncRepository {
    /** Merges local and cloud data for [uid] (union of records) and returns the counts moved. */
    suspend fun sync(uid: String): SyncResult
}
