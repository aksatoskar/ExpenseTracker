package com.example.expensetracker.domain.usecase.identity

import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.feature.FeatureFlags
import com.example.expensetracker.data.classification.BundledClassificationRulesTestFixtures
import com.example.expensetracker.domain.classification.CompiledClassificationRules
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.repository.InstallationIdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InitializeAppIdentityUseCaseTest {

    @Test
    fun `binds installation id to analytics and refreshes feature flags`() = runBlocking {
        val analytics = RecordingAnalytics()
        val crashReporter = RecordingCrashReporter()
        val featureFlags = RecordingFeatureFlags()
        val classificationConfig = RecordingClassificationConfig()
        val useCase = InitializeAppIdentityUseCase(
            installationIdRepository = FakeInstallationIdRepository("fid-123"),
            featureFlagsRepository = featureFlags,
            classificationConfigRepository = classificationConfig,
            analytics = analytics,
            crashReporter = crashReporter
        )

        useCase()

        assertEquals("fid-123", analytics.lastInstallationId)
        assertEquals("fid-123", crashReporter.lastUserId)
        assertEquals(1, featureFlags.refreshCount)
        assertEquals(1, classificationConfig.refreshCount)
    }

    private class FakeInstallationIdRepository(private val id: String) : InstallationIdRepository {
        override val installationId = flowOf(id)
        override suspend fun ensureInstallationId(): String = id
    }

    private class RecordingFeatureFlags : FeatureFlagsRepository {
        var refreshCount = 0
        override val featureFlags = MutableStateFlow(FeatureFlags())
        override suspend fun refresh() { refreshCount++ }
        override fun current(): FeatureFlags = featureFlags.value
    }

    private class RecordingClassificationConfig : ClassificationConfigRepository {
        var refreshCount = 0
        private val rules = BundledClassificationRulesTestFixtures.compiledRules()
        override fun current(): CompiledClassificationRules = rules
        override suspend fun refresh() { refreshCount++ }
    }

    private class RecordingAnalytics : AnalyticsTracker {
        var lastInstallationId: String? = null
        override fun logScreen(screenName: String) {}
        override fun log(event: com.example.expensetracker.domain.analytics.AnalyticsEvent) {}
        override fun setInstallationId(installationId: String) {
            lastInstallationId = installationId
        }
    }

    private class RecordingCrashReporter : CrashReporter {
        var lastUserId: String? = null
        override fun recordNonFatal(throwable: Throwable) {}
        override fun log(message: String) {}
        override fun setKey(key: String, value: String) {}
        override fun setUserId(userId: String) {
            lastUserId = userId
        }
    }
}
