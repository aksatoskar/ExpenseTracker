package com.example.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

/** User preferences and small app-state flags, persisted via DataStore. */
interface SettingsRepository {
    val onboardingComplete: Flow<Boolean>
    val darkTheme: Flow<Boolean>
    val lastSmsSync: Flow<Long>
    val lastBudgetArchiveMonth: Flow<String?>

    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setLastSmsSync(millis: Long)
    suspend fun setLastBudgetArchiveMonth(yearMonth: String)
}
