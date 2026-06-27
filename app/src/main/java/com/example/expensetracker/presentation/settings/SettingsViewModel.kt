package com.example.expensetracker.presentation.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.auth.GoogleCredentialClient
import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.auth.AuthRepository
import com.example.expensetracker.domain.auth.AuthUser
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.feature.FeatureFlags
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.repository.InstallationIdRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.usecase.sms.SyncSmsInboxUseCase
import com.example.expensetracker.domain.usecase.sync.SyncDataUseCase
import com.example.expensetracker.domain.usecase.sync.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs the Settings screen and the dashboard's sync card: theme toggle, manual SMS sync, the test
 * notification, and Google account sign-in plus cloud sync. Activity-scoped so surfaces share state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncSmsInbox: SyncSmsInboxUseCase,
    private val notifier: Notifier,
    private val analytics: AnalyticsTracker,
    private val authRepository: AuthRepository,
    private val googleCredentialClient: GoogleCredentialClient,
    private val syncData: SyncDataUseCase,
    private val installationIdRepository: InstallationIdRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
    detectedMessageRepository: DetectedMessageRepository
) : ViewModel() {

    init {
        viewModelScope.launch { runCatching { featureFlagsRepository.refresh() } }
    }

    val installationId: StateFlow<String?> =
        installationIdRepository.installationId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val featureFlags: StateFlow<FeatureFlags> =
        featureFlagsRepository.featureFlags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureFlags())

    val detectedMessageCount: StateFlow<Int> =
        detectedMessageRepository.count.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val darkTheme: StateFlow<Boolean> =
        settingsRepository.darkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastSmsSync: StateFlow<Long> =
        settingsRepository.lastSmsSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val currentUser: StateFlow<AuthUser?> =
        authRepository.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUserOrNull())

    val lastCloudSync: StateFlow<Long> =
        settingsRepository.lastCloudSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    /** Syncs today's SMS inbox. [onResult] receives the new-transaction count (or -1 if not permitted). */
    fun syncToday(onResult: (Int) -> Unit) = syncSms(DateRange.today(), onResult)

    /** Syncs SMS for an inclusive custom date range. */
    fun syncCustom(from: LocalDate, to: LocalDate, onResult: (Int) -> Unit) =
        syncSms(DateRange.between(from, to), onResult)

    private fun syncSms(range: DateRange, onResult: (Int) -> Unit) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            val count = runCatching { syncSmsInbox(range) }.getOrDefault(0)
            if (count >= 0) analytics.log(AnalyticsEvent.SmsSynced(count))
            _isSyncing.value = false
            onResult(count)
        }
    }

    fun sendTestNotification(): Boolean = notifier.showTest()

    /** Launches the Google account picker and signs in; [onResult] receives a user-facing message. */
    fun signIn(activity: Activity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val token = googleCredentialClient.getGoogleIdToken(activity).getOrElse {
                onResult("Sign-in cancelled")
                return@launch
            }
            authRepository.signInWithGoogle(token).fold(
                onSuccess = { onResult("Signed in as ${it.email ?: it.displayName ?: "user"}") },
                onFailure = { onResult("Sign-in failed: ${it.message ?: "unknown error"}") }
            )
        }
    }

    /** Runs a cloud sync for the signed-in user; [onResult] receives a user-facing message. */
    fun cloudSyncNow(onResult: (String) -> Unit) {
        if (_isCloudSyncing.value) return
        _isCloudSyncing.value = true
        viewModelScope.launch {
            val message = when (val outcome = syncData()) {
                is SyncOutcome.NotSignedIn -> "Sign in to sync"
                is SyncOutcome.Failed -> "Sync failed: ${outcome.error.message ?: "unknown error"}"
                is SyncOutcome.Success ->
                    "Synced (${outcome.result.pushed} up, ${outcome.result.pulled} down)"
            }
            settingsRepository.setSyncPromptShown(true)
            _isCloudSyncing.value = false
            onResult(message)
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
