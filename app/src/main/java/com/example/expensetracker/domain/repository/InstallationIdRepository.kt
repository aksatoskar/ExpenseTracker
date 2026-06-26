package com.example.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

/** Stable Firebase Installation ID for this app install. */
interface InstallationIdRepository {
    val installationId: Flow<String?>

    /** Returns the persisted Firebase Installation ID, fetching and caching it on first access. */
    suspend fun ensureInstallationId(): String
}
