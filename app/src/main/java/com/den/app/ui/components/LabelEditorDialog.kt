package com.den.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.den.app.data.model.Label

@Composable
fun LabelEditorDialog(
    editing: Label?,
    onSave: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(editing?.id) { mutableStateOf(editing?.name ?: "") }
    var colorIndex by rememberSaveable(editing?.id) { mutableStateOf(editing?.colorIndex ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "New label" else "Edit label") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    paletteColors.forEachIndexed { index, color ->
                        ColorDot(
                            color = color,
                            selected = colorIndex == index,
                            onClick = { colorIndex = index },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), colorIndex)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ColorDot(
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    } else {
        Modifier
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .padding(2.dp)
            .then(border)
            .size(32.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick),
    )
}