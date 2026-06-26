package com.example.expensetracker.domain.usecase.identity

import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.repository.InstallationIdRepository
import javax.inject.Inject

/**
 * Ensures this install has a stable Firebase Installation ID, attaches it to analytics/crash
 * reporting, and refreshes Remote Config feature flags.
 */
class InitializeAppIdentityUseCase @Inject constructor(
    private val installationIdRepository: InstallationIdRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter
) {
    suspend operator fun invoke() {
        val installationId = installationIdRepository.ensureInstallationId()
        analytics.setInstallationId(installationId)
        crashReporter.setUserId(installationId)
        crashReporter.setKey("installation_id", installationId)
        featureFlagsRepository.refresh()
    }
}
