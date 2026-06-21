package com.example.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.usecase.sms.SyncSmsInboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings screen and the dashboard's sync card: theme toggle, manual SMS sync and the
 * test notification. Activity-scoped so both surfaces share the same sync state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncSmsInbox: SyncSmsInboxUseCase,
    private val notifier: Notifier
) : ViewModel() {

    val darkTheme: StateFlow<Boolean> =
        settingsRepository.darkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastSmsSync: StateFlow<Long> =
        settingsRepository.lastSmsSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    /** Runs an SMS sync; [onResult] receives the new-transaction count (or -1 if not permitted). */
    fun syncNow(onResult: (Int) -> Unit) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            val count = runCatching { syncSmsInbox() }.getOrDefault(0)
            _isSyncing.value = false
            onResult(count)
        }
    }

    fun sendTestNotification(): Boolean = notifier.showTest()
}
