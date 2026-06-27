package com.example.expensetracker.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.expensetracker.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed [SettingsRepository].
 *
 * A [SharedPreferencesMigration] moves the legacy `expense_prefs` values (written by earlier
 * versions of the app) into DataStore on first access, so existing installs keep their state.
 */
private const val LEGACY_PREFS = "expense_prefs"

private val Context.settingsDataStore by preferencesDataStore(
    name = "expense_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = LEGACY_PREFS
            )
        )
    }
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val store = context.settingsDataStore

    override val onboardingComplete: Flow<Boolean> =
        store.data.map { it[KEY_ONBOARDING] ?: false }

    override val darkTheme: Flow<Boolean> =
        store.data.map { it[KEY_DARK_THEME] ?: false }

    override val lastSmsSync: Flow<Long> =
        store.data.map { it[KEY_LAST_SYNC] ?: 0L }

    override val smsSyncBaseline: Flow<Long?> =
        store.data.map { it[KEY_SMS_BASELINE] }

    override val lastBudgetArchiveMonth: Flow<String?> =
        store.data.map { it[KEY_LAST_ARCHIVE] }

    override val lastCloudSync: Flow<Long> =
        store.data.map { it[KEY_LAST_CLOUD_SYNC] ?: 0L }

    override val syncPromptShown: Flow<Boolean> =
        store.data.map { it[KEY_SYNC_PROMPT_SHOWN] ?: false }

    override val installationId: Flow<String?> =
        store.data.map { it[KEY_INSTALLATION_ID] }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        store.edit { it[KEY_ONBOARDING] = complete }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        store.edit { it[KEY_DARK_THEME] = enabled }
    }

    override suspend fun setLastSmsSync(millis: Long) {
        store.edit { it[KEY_LAST_SYNC] = millis }
    }

    override suspend fun setLastBudgetArchiveMonth(yearMonth: String) {
        store.edit { it[KEY_LAST_ARCHIVE] = yearMonth }
    }

    override suspend fun setLastCloudSync(millis: Long) {
        store.edit { it[KEY_LAST_CLOUD_SYNC] = millis }
    }

    override suspend fun setSyncPromptShown(shown: Boolean) {
        store.edit { it[KEY_SYNC_PROMPT_SHOWN] = shown }
    }

    override suspend fun setInstallationId(id: String) {
        store.edit { it[KEY_INSTALLATION_ID] = id }
    }

    override suspend fun ensureSmsSyncBaseline(now: Long): Long {
        val prefs = store.edit { if (it[KEY_SMS_BASELINE] == null) it[KEY_SMS_BASELINE] = now }
        return prefs[KEY_SMS_BASELINE] ?: now
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_LAST_SYNC = longPreferencesKey("last_sms_sync")
        val KEY_LAST_ARCHIVE = stringPreferencesKey("last_budget_archive_month")
        val KEY_LAST_CLOUD_SYNC = longPreferencesKey("last_cloud_sync")
        val KEY_SYNC_PROMPT_SHOWN = booleanPreferencesKey("sync_prompt_shown")
        val KEY_SMS_BASELINE = longPreferencesKey("sms_sync_baseline")
        val KEY_INSTALLATION_ID = stringPreferencesKey("firebase_installation_id")
    }
}
