package com.example.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

/** User preferences and small app-state flags, persisted via DataStore. */
interface SettingsRepository {
    val onboardingComplete: Flow<Boolean>
    val darkTheme: Flow<Boolean>
    val lastSmsSync: Flow<Long>
    val lastBudgetArchiveMonth: Flow<String?>
    val lastCloudSync: Flow<Long>
    val syncPromptShown: Flow<Boolean>

    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setLastSmsSync(millis: Long)
    suspend fun setLastBudgetArchiveMonth(yearMonth: String)
    suspend fun setLastCloudSync(millis: Long)
    suspend fun setSyncPromptShown(shown: Boolean)
}
