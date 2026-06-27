package com.example.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

/** User preferences and small app-state flags, persisted via DataStore. */
interface SettingsRepository {
    val onboardingComplete: Flow<Boolean>
    val darkTheme: Flow<Boolean>
    val lastSmsSync: Flow<Long>
    val smsSyncBaseline: Flow<Long?>
    val lastBudgetArchiveMonth: Flow<String?>
    val lastCloudSync: Flow<Long>
    val syncPromptShown: Flow<Boolean>
    val installationId: Flow<String?>

    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setLastSmsSync(millis: Long)
    suspend fun setLastBudgetArchiveMonth(yearMonth: String)
    suspend fun setLastCloudSync(millis: Long)
    suspend fun setSyncPromptShown(shown: Boolean)
    suspend fun setInstallationId(id: String)

    /**
     * Returns the SMS-sync baseline: the earliest timestamp this install will scan from. Set once
     * to [now] on first call and stable afterwards. Because DataStore is cleared on uninstall, a
     * reinstall starts a fresh baseline so old SMS are never re-ingested.
     */
    suspend fun ensureSmsSyncBaseline(now: Long): Long
}
