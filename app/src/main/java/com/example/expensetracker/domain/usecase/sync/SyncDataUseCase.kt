package com.example.expensetracker.domain.usecase.sync

import com.example.expensetracker.core.concurrency.DispatcherProvider
import com.example.expensetracker.domain.auth.AuthRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.sync.SyncRepository
import com.example.expensetracker.domain.sync.SyncResult
import javax.inject.Inject
import kotlinx.coroutines.withContext

/** Result of invoking [SyncDataUseCase]. */
sealed interface SyncOutcome {
    data object NotSignedIn : SyncOutcome
    data class Success(val result: SyncResult) : SyncOutcome
    data class Failed(val error: Throwable) : SyncOutcome
}

/**
 * Runs a two-way sync for the signed-in user and records the completion time.
 * Returns [SyncOutcome.NotSignedIn] if no user is signed in (caller should prompt sign-in).
 */
class SyncDataUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider
) {
    suspend operator fun invoke(): SyncOutcome = withContext(dispatchers.io) {
        val uid = authRepository.currentUserOrNull()?.uid ?: return@withContext SyncOutcome.NotSignedIn
        runCatching { syncRepository.sync(uid) }.fold(
            onSuccess = {
                settingsRepository.setLastCloudSync(System.currentTimeMillis())
                SyncOutcome.Success(it)
            },
            onFailure = { SyncOutcome.Failed(it) }
        )
    }
}
