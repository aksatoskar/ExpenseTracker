package com.example.expensetracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.auth.AuthRepository
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.usecase.budget.RenewBudgetsUseCase
import com.example.expensetracker.domain.usecase.sms.SyncSmsInboxUseCase
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-level UI state: whether settings have loaded, onboarding status and the active theme. */
data class AppUiState(
    val loaded: Boolean = false,
    val onboardingComplete: Boolean = false,
    val darkTheme: Boolean = false
)

/**
 * Owns cross-cutting app state (theme, onboarding) and one-time startup tasks (SMS recovery sync
 * and monthly budget renewal). Scoped to the activity so every screen shares one instance.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncSmsInbox: SyncSmsInboxUseCase,
    private val renewBudgets: RenewBudgetsUseCase,
    private val analytics: AnalyticsTracker,
    private val authRepository: AuthRepository,
    private val cloudSyncScheduler: CloudSyncScheduler,
    private val featureFlagsRepository: FeatureFlagsRepository
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        settingsRepository.onboardingComplete,
        settingsRepository.darkTheme
    ) { onboarded, dark ->
        AppUiState(loaded = true, onboardingComplete = onboarded, darkTheme = dark)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    /** True only while a signed-in user has not yet been shown the one-time post-login sync prompt. */
    val showSyncPrompt: StateFlow<Boolean> = combine(
        authRepository.currentUser,
        settingsRepository.syncPromptShown,
        featureFlagsRepository.featureFlags
    ) { user, shown, flags -> user != null && !shown && flags.cloudSyncEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var startupTriggered = false

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }

    /** Dismisses the one-time sync prompt; when [sync] is true, also queues a background cloud sync. */
    fun resolveSyncPrompt(sync: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncPromptShown(true)
            if (sync) cloudSyncScheduler.schedule()
        }
    }

    /** Reports a bottom-nav screen view to analytics. */
    fun logScreen(name: String) = analytics.logScreen(name)

    /** Runs once per process: recover missed SMS transactions, renew budgets, and queue cloud sync. */
    fun runStartupTasks() {
        if (startupTriggered) return
        startupTriggered = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { syncSmsInbox() }
                .getOrNull()
                ?.takeIf { it >= 0 }
                ?.let { analytics.log(AnalyticsEvent.SmsSynced(it)) }
            runCatching { renewBudgets() }
            cloudSyncScheduler.schedule()
        }
    }
}
