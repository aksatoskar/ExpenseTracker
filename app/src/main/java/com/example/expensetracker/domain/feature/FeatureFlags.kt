package com.example.expensetracker.domain.feature

/** Remote Config keys toggled per installation segment in Firebase Console. */
object FeatureFlagKeys {
    const val CLOUD_SYNC = "feature_cloud_sync_enabled"
    const val DETECTED_MESSAGES = "feature_detected_messages_enabled"
}

/** Snapshot of remote feature toggles for this installation. */
data class FeatureFlags(
    val cloudSyncEnabled: Boolean = true,
    val detectedMessagesEnabled: Boolean = true
)
