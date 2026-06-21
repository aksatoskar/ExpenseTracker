package com.example.expensetracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.usecase.budget.RenewBudgetsUseCase
import com.example.expensetracker.domain.usecase.sms.SyncSmsInboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val renewBudgets: RenewBudgetsUseCase
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        settingsRepository.onboardingComplete,
        settingsRepository.darkTheme
    ) { onboarded, dark ->
        AppUiState(loaded = true, onboardingComplete = onboarded, darkTheme = dark)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private var startupTriggered = false

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }

    /** Runs once per process: recover missed SMS transactions and renew budgets if needed. */
    fun runStartupTasks() {
        if (startupTriggered) return
        startupTriggered = true
        viewModelScope.launch {
            runCatching { syncSmsInbox() }
            runCatching { renewBudgets() }
        }
    }
}
