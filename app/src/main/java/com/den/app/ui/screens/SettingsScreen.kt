package com.den.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.ColorDot
import com.den.app.ui.components.ColorPickerDialog
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.LocalSnackbarHostState
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.paletteColors
import com.den.app.ui.theme.PALETTES
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.SettingsViewModel
import com.den.app.util.Dates
import com.den.app.util.Fmt
import kotlinx.coroutines.launch

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
            DenTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
            SettingsSection("Appearance")
            SettingRow("Theme color", trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showColorPicker = true }) {
                    ColorDot(color = paletteColors.getOrElse(settings.themeColorIndex) { Color.Transparent }, selected = false, onClick = { showColorPicker = true })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = PALETTES.getOrNull(settings.themeColorIndex)?.name ?: "Color",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            })
            SettingRow(
                label = "Dark mode",
                subtitle = "How Den looks on this device",
                content = {
                    FilterChipRow(
                        items = listOf(
                            ChipItem("Auto", settings.darkMode == 0) { vm.setDarkMode(0) },
                            ChipItem("Light", settings.darkMode == 1) { vm.setDarkMode(1) },
                            ChipItem("Dark", settings.darkMode == 2) { vm.setDarkMode(2) },
                        ),
                    )
                },
            )
            SettingRow("Dynamic color", subtitle = "Use wallpaper colors (Android 12+)", trailing = {
                Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamicColor)
            })

            SettingsSection("Completion")
            SettingRow("Rate tasks on completion", subtitle = "Ask for a rating, note and photo when finishing a task", trailing = {
                Switch(checked = settings.ratingOnComplete, onCheckedChange = vm::setRatingOnComplete)
            })

            SettingsSection("Reminders")
            SettingRow(
                label = "Default reminder",
                subtitle = "Applied when adding tasks with a due date",
                content = {
                    FilterChipRow(
                        items = listOf(
                            ChipItem("Off", settings.reminderDefaultMinutes == 0) { vm.setReminderDefaultMinutes(0) },
                            ChipItem("5m", settings.reminderDefaultMinutes == 5) { vm.setReminderDefaultMinutes(5) },
                            ChipItem("10m", settings.reminderDefaultMinutes == 10) { vm.setReminderDefaultMinutes(10) },
                            ChipItem("15m", settings.reminderDefaultMinutes == 15) { vm.setReminderDefaultMinutes(15) },
                            ChipItem("30m", settings.reminderDefaultMinutes == 30) { vm.setReminderDefaultMinutes(30) },
                            ChipItem("1h", settings.reminderDefaultMinutes == 60) { vm.setReminderDefaultMinutes(60) },
                        ),
                    )
                },
            )

            SettingsSection("Security")
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

            SettingsSection("Backups")
            SettingRow("Auto backup", subtitle = "Encrypted backups stored on device", trailing = {
                Switch(checked = settings.backupEnabled, onCheckedChange = vm::setBackupEnabled)
            })
            SettingRow(
                label = "Frequency",
                content = {
                    FilterChipRow(
                        items = listOf(
                            ChipItem("Daily", settings.backupFrequency == 0) { vm.setBackupFrequency(0) },
                            ChipItem("Weekly", settings.backupFrequency == 1) { vm.setBackupFrequency(1) },
                        ),
                    )
                },
            )
            SettingRow("Time", subtitle = "Automatic backups run around this hour", trailing = {
                AssistChip(onClick = { showHourPicker = true }, label = { Text(hourLabel(settings.backupHour)) })
            })
            SettingRow("Include media", subtitle = "Photos & videos in backups (larger files)", trailing = {
                Switch(checked = settings.backupIncludeMedia, onCheckedChange = vm::setBackupIncludeMedia)
            })
            SettingRow(
                label = "Keep backups",
                content = {
                    FilterChipRow(
                        items = listOf(2, 5, 10, 20).map { count ->
                            ChipItem("$count", settings.backupKeepCount == count) { vm.setBackupKeepCount(count) }
                        },
                    )
                },
            )
            SettingRow(
                label = "Encryption",
                subtitle = "How backup files are encrypted",
                content = {
                    FilterChipRow(
                        items = listOf(
                            ChipItem("Device key", settings.backupEncryption == "device") { vm.setBackupEncryption("device") },
                            ChipItem("Passphrase", settings.backupEncryption == "passphrase") { vm.setBackupEncryption("passphrase") },
                        ),
                    )
                },
            )
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

            SettingsSection("Data")
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

            SettingsSection("Portable")
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
        ColorPickerDialog(
            title = "Theme color",
            selected = settings.themeColorIndex,
            allowNone = false,
            onSelect = { vm.setThemeColorIndex(it ?: 0) },
            onDismiss = { showColorPicker = false },
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
                            null -> {}
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
private fun SettingsSection(text: String) =
    SectionHeader(text, Modifier.padding(top = 24.dp, bottom = 6.dp))

@Composable
private fun SettingRow(
    label: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        if (content != null) {
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun hourLabel(hour: Int): String = String.format("%02d:00", hour)