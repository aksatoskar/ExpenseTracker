package com.example.expensetracker.data.identity

import com.example.expensetracker.domain.repository.InstallationIdRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Firebase Installations ID (FID): unique per app install, stable across app restarts until
 * uninstall or app-data clear. Cached locally so Settings can show it immediately.
 */
@Singleton
class FirebaseInstallationIdRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) : InstallationIdRepository {

    override val installationId: Flow<String?> = settingsRepository.installationId

    override suspend fun ensureInstallationId(): String {
        val firebaseId = FirebaseInstallations.getInstance().id.await()
        val cached = settingsRepository.installationId.first()
        if (cached != firebaseId) {
            settingsRepository.setInstallationId(firebaseId)
        }
        return firebaseId
    }
}
