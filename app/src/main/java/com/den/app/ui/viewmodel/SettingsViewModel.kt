package com.den.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.settings.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val settingsStore = container.settings
    private val backupManager = container.backupManager
    val passcode = container.passcode

    val settings: StateFlow<Settings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private val _backups = MutableStateFlow<List<File>>(emptyList())
    val backups: StateFlow<List<File>> = _backups

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toast.asSharedFlow()

    fun refreshBackups() {
        viewModelScope.launch { _backups.value = backupManager.files() }
    }

    private fun notify(message: String) {
        viewModelScope.launch { _toast.emit(message) }
    }

    fun setThemeColorIndex(v: Int) = safe { settingsStore.setThemeColorIndex(v) }
    fun setDarkMode(v: Int) = safe { settingsStore.setDarkMode(v) }
    fun setDynamicColor(v: Boolean) = safe { settingsStore.setDynamicColor(v) }
    fun setRatingOnComplete(v: Boolean) = safe { settingsStore.setRatingOnComplete(v) }
    fun setBackupEnabled(v: Boolean) = safe { settingsStore.setBackupEnabled(v) }
    fun setBackupFrequency(v: Int) = safe { settingsStore.setBackupFrequency(v) }
    fun setBackupHour(v: Int) = safe { settingsStore.setBackupHour(v) }
    fun setBackupIncludeMedia(v: Boolean) = safe { settingsStore.setBackupIncludeMedia(v) }
    fun setBackupKeepCount(v: Int) = safe { settingsStore.setBackupKeepCount(v) }
    fun setBackupEncryption(v: String) = safe { settingsStore.setBackupEncryption(v) }
    fun setBackupPassphrase(v: String) = safe { settingsStore.setBackupPassphrase(v) }
    fun setReminderDefaultMinutes(v: Int) = safe { settingsStore.setReminderDefaultMinutes(v) }

    fun backupNow() {
        viewModelScope.launch {
            val result = backupManager.createAutoBackup(force = true)
            notify(if (result.isSuccess) "Backup created" else "Backup failed: ${result.exceptionOrNull()?.message}")
            refreshBackups()
        }
    }

    fun exportBackup(uri: Uri, includeMedia: Boolean) {
        viewModelScope.launch {
            val result = backupManager.exportToUri(uri, includeMedia)
            notify(if (result.isSuccess) "Backup exported" else "Export failed: ${result.exceptionOrNull()?.message}")
        }
    }

    fun restoreBackup(uri: Uri, passphrase: String?) {
        viewModelScope.launch {
            val result = backupManager.restoreFromUri(uri, passphrase)
            notify(if (result.isSuccess) "Restore completed" else "Restore failed: ${result.exceptionOrNull()?.message}")
            refreshBackups()
        }
    }

    fun enablePasscode(code: String) {
        viewModelScope.launch {
            if (code.length < 4) {
                notify("Use at least 4 digits")
                return@launch
            }
            passcode.enable(code)
            notify("Passcode enabled")
        }
    }

    fun disablePasscode() {
        viewModelScope.launch {
            passcode.disable()
            notify("Passcode removed")
        }
    }

    suspend fun verifyPasscode(code: String): Boolean = passcode.verify(code)

    private fun safe(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}