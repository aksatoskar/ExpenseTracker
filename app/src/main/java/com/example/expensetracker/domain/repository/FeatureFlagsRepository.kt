package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.feature.FeatureFlags
import kotlinx.coroutines.flow.Flow

/** Firebase Remote Config-backed feature toggles for this installation. */
interface FeatureFlagsRepository {
    val featureFlags: Flow<FeatureFlags>

    /** Fetches the latest values from Firebase and updates the local snapshot. */
    suspend fun refresh()

    fun current(): FeatureFlags
}
