package com.example.expensetracker.data.feature

import android.content.pm.ApplicationInfo
import com.example.expensetracker.domain.feature.FeatureFlagKeys
import com.example.expensetracker.domain.feature.FeatureFlags
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

/** Reads feature toggles from Firebase Remote Config for this installation. */
@Singleton
class FirebaseFeatureFlagsRepository @Inject constructor(
    @ApplicationContext context: Context,
    remoteConfig: FirebaseRemoteConfig
) : FeatureFlagsRepository {

    private val remoteConfig: FirebaseRemoteConfig = remoteConfig.apply {
        val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (debug) 0 else 12 * 60 * 60)
                .build()
        )
        setDefaultsAsync(
            mapOf(
                FeatureFlagKeys.CLOUD_SYNC to true,
                FeatureFlagKeys.DETECTED_MESSAGES to true
            )
        )
    }

    private val _featureFlags = MutableStateFlow(readFlags())
    override val featureFlags: Flow<FeatureFlags> = _featureFlags.asStateFlow()

    override suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
        _featureFlags.value = readFlags()
    }

    override fun current(): FeatureFlags = _featureFlags.value

    private fun readFlags(): FeatureFlags = FeatureFlags(
        cloudSyncEnabled = remoteConfig.getBoolean(FeatureFlagKeys.CLOUD_SYNC),
        detectedMessagesEnabled = remoteConfig.getBoolean(FeatureFlagKeys.DETECTED_MESSAGES)
    )
}
