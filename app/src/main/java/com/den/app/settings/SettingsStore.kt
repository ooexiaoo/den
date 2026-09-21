package com.den.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "den_settings")

data class Settings(
    val themeColorIndex: Int = 0,
    val darkMode: Int = 0,
    val dynamicColor: Boolean = false,
    val passcodeEnabled: Boolean = false,
    val passcodeHash: String? = null,
    val ratingOnComplete: Boolean = true,
    val backupEnabled: Boolean = true,
    val backupFrequency: Int = 0,
    val backupHour: Int = 2,
    val backupIncludeMedia: Boolean = true,
    val backupKeepCount: Int = 5,
    val backupEncryption: String = "device",
    val backupPassphrase: String = "",
    val lastBackupAt: Long = 0L,
    val onboardingDone: Boolean = false,
    val reminderDefaultMinutes: Int = 10,
    val showCompletedByDefault: Boolean = false,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME_COLOR = intPreferencesKey("theme_color")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PASSCODE_ENABLED = booleanPreferencesKey("passcode_enabled")
        val PASSCODE_HASH = stringPreferencesKey("passcode_hash")
        val RATING_ON_COMPLETE = booleanPreferencesKey("rating_on_complete")
        val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        val BACKUP_FREQUENCY = intPreferencesKey("backup_frequency")
        val BACKUP_HOUR = intPreferencesKey("backup_hour")
        val BACKUP_INCLUDE_MEDIA = booleanPreferencesKey("backup_include_media")
        val BACKUP_KEEP_COUNT = intPreferencesKey("backup_keep_count")
        val BACKUP_ENCRYPTION = stringPreferencesKey("backup_encryption")
        val BACKUP_PASSPHRASE = stringPreferencesKey("backup_passphrase")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REMINDER_DEFAULT_MINUTES = intPreferencesKey("reminder_default_minutes")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): Settings = Settings(
        themeColorIndex = this[Keys.THEME_COLOR] ?: 0,
        darkMode = this[Keys.DARK_MODE] ?: 0,
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: false,
        passcodeEnabled = this[Keys.PASSCODE_ENABLED] ?: false,
        passcodeHash = this[Keys.PASSCODE_HASH],
        ratingOnComplete = this[Keys.RATING_ON_COMPLETE] ?: true,
        backupEnabled = this[Keys.BACKUP_ENABLED] ?: true,
        backupFrequency = this[Keys.BACKUP_FREQUENCY] ?: 0,
        backupHour = this[Keys.BACKUP_HOUR] ?: 2,
        backupIncludeMedia = this[Keys.BACKUP_INCLUDE_MEDIA] ?: true,
        backupKeepCount = this[Keys.BACKUP_KEEP_COUNT] ?: 5,
        backupEncryption = this[Keys.BACKUP_ENCRYPTION] ?: "device",
        backupPassphrase = this[Keys.BACKUP_PASSPHRASE] ?: "",
        lastBackupAt = this[Keys.LAST_BACKUP_AT] ?: 0L,
        onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
        reminderDefaultMinutes = this[Keys.REMINDER_DEFAULT_MINUTES] ?: 10,
        showCompletedByDefault = this[Keys.SHOW_COMPLETED] ?: false,
    )

    suspend fun setThemeColorIndex(v: Int) = edit { it[Keys.THEME_COLOR] = v }
    suspend fun setDarkMode(v: Int) = edit { it[Keys.DARK_MODE] = v }
    suspend fun setDynamicColor(v: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = v }
    suspend fun setPasscodeEnabled(v: Boolean) = edit { it[Keys.PASSCODE_ENABLED] = v }
    suspend fun setPasscodeHash(v: String?) = edit { if (v == null) it.remove(Keys.PASSCODE_HASH) else it[Keys.PASSCODE_HASH] = v }
    suspend fun setRatingOnComplete(v: Boolean) = edit { it[Keys.RATING_ON_COMPLETE] = v }
    suspend fun setBackupEnabled(v: Boolean) = edit { it[Keys.BACKUP_ENABLED] = v }
    suspend fun setBackupFrequency(v: Int) = edit { it[Keys.BACKUP_FREQUENCY] = v }
    suspend fun setBackupHour(v: Int) = edit { it[Keys.BACKUP_HOUR] = v }
    suspend fun setBackupIncludeMedia(v: Boolean) = edit { it[Keys.BACKUP_INCLUDE_MEDIA] = v }
    suspend fun setBackupKeepCount(v: Int) = edit { it[Keys.BACKUP_KEEP_COUNT] = v }
    suspend fun setBackupEncryption(v: String) = edit { it[Keys.BACKUP_ENCRYPTION] = v }
    suspend fun setBackupPassphrase(v: String) = edit { it[Keys.BACKUP_PASSPHRASE] = v }
    suspend fun setLastBackupAt(v: Long) = edit { it[Keys.LAST_BACKUP_AT] = v }
    suspend fun setOnboardingDone(v: Boolean) = edit { it[Keys.ONBOARDING_DONE] = v }
    suspend fun setReminderDefaultMinutes(v: Int) = edit { it[Keys.REMINDER_DEFAULT_MINUTES] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}