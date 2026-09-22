package com.den.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.ColorDot
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.paletteColors
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskEditViewModel
import com.den.app.util.Dates
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    container: AppContainer,
    taskId: Long?,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val vm: TaskEditViewModel = viewModel(factory = DenViewModelFactory(container, id = taskId))
    val ui by vm.ui.collectAsState()
    val labels by vm.allLabels.collectAsState()
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch { onSaved(vm.save()) }
                        },
                        enabled = ui.title.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
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
            OutlinedTextField(
                value = ui.title,
                onValueChange = vm::setTitle,
                label = { Text("Task") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = ui.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Due")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(if (ui.hasDue) Dates.formatDate(ui.dueAt!!) else "Set date") },
                )
                if (ui.hasDue) {
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(ui.dueAt?.let { Dates.formatTime(it) } ?: "Add time") },
                    )
                    IconButton(onClick = vm::onDueClear, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear due", modifier = Modifier.size(18.dp))
                    }
                }
            }

            SectionLabel("Reminder")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Remind me", style = MaterialTheme.typography.bodyMedium)
                    if (ui.hasReminder && ui.reminderAt != null) {
                        Text(
                            Dates.formatDateTime(ui.reminderAt!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(
                    checked = ui.hasReminder,
                    onCheckedChange = { on ->
                        if (on) {
                            val due = ui.dueAt
                            val base = due?.let { it - 10 * 60 * 1000L } ?: (System.currentTimeMillis() + 30 * 60 * 1000L)
                            vm.onReminderSet(base)
                            showReminderPicker = true
                        } else {
                            vm.onReminderClear()
                        }
                    },
                )
            }

            SectionLabel("Priority")
            FilterChipRow(
                items = listOf(
                    ChipItem("None", ui.priority == 0) { vm.setPriority(0) },
                    ChipItem("Low", ui.priority == 1) { vm.setPriority(1) },
                    ChipItem("Medium", ui.priority == 2) { vm.setPriority(2) },
                    ChipItem("High", ui.priority == 3) { vm.setPriority(3) },
                ),
            )

            SectionLabel("Color")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                paletteColors.forEachIndexed { index, color ->
                    ColorDot(
                        color = color,
                        selected = ui.colorIndex == index,
                        onClick = { vm.setColor(if (ui.colorIndex == index) null else index) },
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            SectionLabel("Labels")
            if (labels.isEmpty()) {
                Text(
                    "Create labels from the Labels tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilterChipRow(
                    items = labels.map { l ->
                        ChipItem(
                            label = l.label.name,
                            selected = l.label.id in ui.selectedLabels,
                            onClick = { vm.toggleLabel(l.label.id) },
                        )
                    },
                )
            }

            SectionLabel("Subtasks")
            ui.subtasks.forEach { sub ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = sub.done,
                        onCheckedChange = { vm.toggleSubtaskDone(sub.id, it) },
                    )
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (sub.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { vm.reorderSubtask(sub.id, -1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { vm.reorderSubtask(sub.id, 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { vm.removeSubtask(sub) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            var newSubtask by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    placeholder = { Text("Add subtask") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        vm.addSubtaskLocally(newSubtask)
                        newSubtask = ""
                    },
                    enabled = newSubtask.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add subtask")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = ui.dueAt?.let { utcOfDay(it) },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = dateState.selectedDateMillis
                        if (selected != null) {
                            vm.onDueSet(combineDateTime(selected, ui.dueAt))
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        val base = ui.dueAt ?: Dates.fromLocal(LocalDateTime.now().withHour(18).withMinute(0))
        val ldt = Dates.toLocal(base)
        val timeState = rememberTimePickerState(initialHour = ldt.hour, initialMinute = ldt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.onDueSet(
                            Dates.fromLocal(
                                LocalDateTime.of(
                                    Dates.toLocal(base).toLocalDate(),
                                    LocalTime.of(timeState.hour, timeState.minute),
                                )
                            )
                        )
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }

    if (showReminderPicker) {
        val base = (ui.reminderAt ?: ui.dueAt) ?: (System.currentTimeMillis() + 60 * 60 * 1000L)
        val ldt = Dates.toLocal(base)
        val timeState = rememberTimePickerState(initialHour = ldt.hour, initialMinute = ldt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showReminderPicker = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.onReminderSet(
                            Dates.fromLocal(
                                LocalDateTime.of(Dates.toLocal(base).toLocalDate(), LocalTime.of(timeState.hour, timeState.minute))
                            )
                        )
                        showReminderPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderPicker = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) =
    SectionHeader(text, Modifier.padding(top = 20.dp, bottom = 8.dp))

private fun utcOfDay(millis: Long): Long =
    Dates.toLocal(millis).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun combineDateTime(utcDateMillis: Long, current: Long?): Long {
    val date = Instant.ofEpochMilli(utcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val time = current?.let { Dates.toLocal(it).toLocalTime() } ?: LocalTime.of(18, 0)
    return Dates.fromLocal(LocalDateTime.of(date, time))
}