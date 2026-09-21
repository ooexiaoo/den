package com.den.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.BuildConfig
import com.den.app.ui.components.ColorDot
import com.den.app.ui.components.LocalSnackbarHostState
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.paletteColors
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.SettingsViewModel
import com.den.app.util.Dates
import com.den.app.util.Fmt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = DenViewModelFactory(container))
    val settings by vm.settings.collectAsState()
    val backups by vm.backups.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.refreshBackups()
    }
    LaunchedEffect(vm) {
        vm.toastEvents.collect { message ->
            scope.launch { snackbar.showSnackbar(message) }
        }
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }
    var showPasscode by remember { mutableStateOf(false) }
    var showDisablePasscode by remember { mutableStateOf(false) }
    var passphraseDialog by remember { mutableStateOf<BackupAction?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingPortImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.exportBackup(uri, settings.backupIncludeMedia)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (settings.backupEncryption == "passphrase") {
                pendingRestoreUri = uri
                passphraseDialog = BackupAction.RESTORE
            } else {
                vm.restoreBackup(uri, null)
            }
        }
    }
    val exportPort = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportPort(uri)
    }
    val openPort = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingPortImportUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionHeader("Appearance")
            SettingRow("Theme color", trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showColorPicker = true }) {
                    ColorDot(color = paletteColors.getOrElse(settings.themeColorIndex) { Color.Transparent }, selected = false, onClick = { showColorPicker = true })
                    Spacer(Modifier.width(6.dp))
                    Text(themeColorName(settings.themeColorIndex), style = MaterialTheme.typography.bodyMedium)
                }
            })
            SettingRow("Dark mode", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Auto", 1 to "Light", 2 to "Dark").forEach { (value, label) ->
                        FilterChip(
                            selected = settings.darkMode == value,
                            onClick = { vm.setDarkMode(value) },
                            label = { Text(label) },
                        )
                    }
                }
            })
            SettingRow("Dynamic color", subtitle = "Use wallpaper colors (Android 12+)", trailing = {
                Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamicColor)
            })

            SectionHeader("Completion")
            SettingRow("Rate tasks on completion", subtitle = "Ask for a rating, note and photo when finishing a task", trailing = {
                Switch(checked = settings.ratingOnComplete, onCheckedChange = vm::setRatingOnComplete)
            })

            SectionHeader("Reminders")
            SettingRow("Default reminder", subtitle = "Applied when adding tasks with a due date", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Off", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (value, label) ->
                        FilterChip(
                            selected = settings.reminderDefaultMinutes == value,
                            onClick = { vm.setReminderDefaultMinutes(value) },
                            label = { Text(label) },
                        )
                    }
                }
            })

            SectionHeader("Security")
            val passcodeOn = settings.passcodeEnabled && !settings.passcodeHash.isNullOrEmpty()
            SettingRow("Passcode lock", subtitle = if (passcodeOn) "Lock the app behind a passcode" else "Off", trailing = {
                if (!passcodeOn) {
                    OutlinedButton(onClick = { showPasscode = true }) { Text("Enable") }
                } else {
                    TextButton(onClick = { showDisablePasscode = true }) {
                        Text("Disable", color = MaterialTheme.colorScheme.error)
                    }
                }
            })
            val context = LocalContext.current
            val biometricAvailable = remember {
                BiometricManager.from(context)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            }
            if (passcodeOn && biometricAvailable) {
                SettingRow("Biometric unlock", subtitle = "Unlock with fingerprint instead of typing", trailing = {
                    Switch(checked = settings.biometricEnabled, onCheckedChange = vm::setBiometricEnabled)
                })
            }

            SectionHeader("Backups")
            SettingRow("Auto backup", subtitle = "Encrypted backups stored on device", trailing = {
                Switch(checked = settings.backupEnabled, onCheckedChange = vm::setBackupEnabled)
            })
            SettingRow("Frequency", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.backupFrequency == 0,
                        onClick = { vm.setBackupFrequency(0) },
                        label = { Text("Daily") },
                    )
                    FilterChip(
                        selected = settings.backupFrequency == 1,
                        onClick = { vm.setBackupFrequency(1) },
                        label = { Text("Weekly") },
                    )
                }
            })
            SettingRow("Time", subtitle = "Automatic backups run around this hour", trailing = {
                AssistChip(onClick = { showHourPicker = true }, label = { Text(hourLabel(settings.backupHour)) })
            })
            SettingRow("Include media", subtitle = "Photos & videos in backups (larger files)", trailing = {
                Switch(checked = settings.backupIncludeMedia, onCheckedChange = vm::setBackupIncludeMedia)
            })
            SettingRow("Keep backups", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 5, 10, 20).forEach { count ->
                        FilterChip(
                            selected = settings.backupKeepCount == count,
                            onClick = { vm.setBackupKeepCount(count) },
                            label = { Text("$count") },
                        )
                    }
                }
            })
            SettingRow("Encryption", subtitle = "How backup files are encrypted", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.backupEncryption == "device",
                        onClick = { vm.setBackupEncryption("device") },
                        label = { Text("Device key") },
                    )
                    FilterChip(
                        selected = settings.backupEncryption == "passphrase",
                        onClick = { vm.setBackupEncryption("passphrase") },
                        label = { Text("Passphrase") },
                    )
                }
            })
            if (settings.backupEncryption == "passphrase") {
                SettingRow("Backup passphrase", subtitle = "Required to restore on another device", trailing = {
                    OutlinedButton(onClick = { passphraseDialog = BackupAction.SET }) {
                        Text(if (settings.backupPassphrase.isEmpty()) "Set" else "Change")
                    }
                })
            }

            FilledTonalButton(
                onClick = { vm.backupNow() },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Backup now")
            }

            if (backups.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent backups", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                backups.take(6).forEach { file ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.formatBackup(file.lastModified()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(Fmt.bytes(file.length()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SectionHeader("Data")
            OutlinedButton(
                onClick = { createBackup.launch("den-backup-${System.currentTimeMillis()}.cbl") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    openBackup.launch(arrayOf("application/octet-stream"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore from backup")
            }
            Text(
                "Restoring replaces all current data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            SectionHeader("Portable")
            OutlinedButton(
                onClick = { exportPort.launch("den-export-${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export all as JSON")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { openPort.launch(arrayOf("application/json", "application/octet-stream", "text/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import from JSON")
            }
            Text(
                "Plain JSON of tasks, notes, subtasks and labels. Importing merges into your data; media is not included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "Den · v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Theme color") },
            text = {
                Column {
                    paletteColors.forEachIndexed { index, color ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { vm.setThemeColorIndex(index); showColorPicker = false }.padding(vertical = 8.dp),
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(color = color, selected = settings.themeColorIndex == index, onClick = {})
                                Spacer(Modifier.width(12.dp))
                                Text(themeColorName(index), style = MaterialTheme.typography.bodyLarge)
                            }
                            if (settings.themeColorIndex == index) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Close") }
            },
        )
    }

    if (showHourPicker) {
        AlertDialog(
            onDismissRequest = { showHourPicker = false },
            title = { Text("Backup time") },
            text = {
                Column {
                    listOf(0, 2, 4, 6, 9, 12, 15, 18, 21, 23).forEach { hour ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { vm.setBackupHour(hour); showHourPicker = false }.padding(vertical = 8.dp),
                        ) {
                            Text(hourLabel(hour), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (settings.backupHour == hour) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHourPicker = false }) { Text("Close") }
            },
        )
    }

    if (showPasscode) {
        var code by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasscode = false },
            title = { Text("Enable passcode") },
            text = {
                Column {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.filter(Char::isDigit).take(8)
                        },
                        label = { Text("4-8 digit passcode") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            confirm = it.filter(Char::isDigit).take(8)
                        },
                        label = { Text("Repeat") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (code.length >= 4 && code == confirm) {
                            vm.enablePasscode(code)
                            showPasscode = false
                        } else {
                            scope.launch { snackbar.showSnackbar("Codes must match and be 4+ digits") }
                        }
                    },
                ) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showPasscode = false }) { Text("Cancel") }
            },
        )
    }

    if (showDisablePasscode) {
        ConfirmDialog(
            title = "Disable passcode?",
            message = "Your data stays encrypted; only the app lock is removed.",
            confirmText = "Disable",
            onConfirm = {
                vm.disablePasscode()
                showDisablePasscode = false
            },
            onDismiss = { showDisablePasscode = false },
        )
    }

    if (pendingPortImportUri != null) {
        ConfirmDialog(
            title = "Import JSON?",
            message = "Tasks, notes and labels from this file will be merged into your current data.",
            confirmText = "Import",
            onConfirm = {
                pendingPortImportUri?.let { vm.importPort(it) }
                pendingPortImportUri = null
            },
            onDismiss = { pendingPortImportUri = null },
        )
    }

    if (passphraseDialog != null) {
        var phrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passphraseDialog = null; pendingRestoreUri = null },
            title = {
                Text(if (passphraseDialog == BackupAction.SET) "Backup passphrase" else "Enter passphrase to restore")
            },
            text = {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text(if (passphraseDialog == BackupAction.SET) "Set a strong passphrase" else "Passphrase used to export") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (passphraseDialog) {
                            BackupAction.SET -> {
                                if (phrase.length >= 8) {
                                    vm.setBackupPassphrase(phrase)
                                    passphraseDialog = null
                                } else {
                                    scope.launch { snackbar.showSnackbar("Use at least 8 characters") }
                                }
                            }
                            BackupAction.RESTORE -> {
                                vm.setBackupPassphrase(phrase)
                                passphraseDialog = null
                                pendingRestoreUri?.let { uri -> vm.restoreBackup(uri, phrase) }
                                pendingRestoreUri = null
                            }
                        }
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { passphraseDialog = null; pendingRestoreUri = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class BackupAction { SET, RESTORE }

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(
    label: String,
    trailing: @Composable () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

private fun themeColorName(index: Int): String =
    listOf("Indigo", "Teal", "Emerald", "Rose", "Amber", "Sky", "Violet", "Crimson", "Orange", "Slate", "Lime", "Cyan", "Grape", "Sunset")
        .getOrElse(index) { "Color" }

private fun hourLabel(hour: Int): String = String.format("%02d:00", hour)