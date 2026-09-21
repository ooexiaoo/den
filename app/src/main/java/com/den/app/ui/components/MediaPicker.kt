package com.den.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MediaPickerBar(
    onImportUri: suspend (Uri) -> Boolean,
    onImportFile: suspend (File, String, String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    fun prepareCapture(ext: String): File {
        val dir = File(context.cacheDir, "media").apply { mkdirs() }
        val file = File(dir, "cap_${System.currentTimeMillis()}.$ext").apply {
            if (exists()) delete()
            createNewFile()
        }
        pendingCapture = file
        return file
    }

    fun providerUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun importOrDiscard(file: File?, saved: Boolean, mime: String) {
        val current = file ?: return
        scope.launch {
            try {
                if (saved) onImportFile(current, mime, current.name)
            } catch (_: Exception) {
            } finally {
                current.delete()
            }
        }
    }

    val images = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        uris.forEach { uri -> scope.launch { runCatching { onImportUri(uri) } } }
    }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { onImportUri(uri) } }
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCapture
        pendingCapture = null
        importOrDiscard(file, saved, "image/jpeg")
    }
    val record = rememberLauncherForActivityResult(ActivityResultContracts.TakeVideo()) { saved ->
        val file = pendingCapture
        pendingCapture = null
        importOrDiscard(file, saved, "video/mp4")
    }

    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = { images.launch() },
            label = { Text("Photos") },
            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
        )
        AssistChip(
            onClick = {
                video.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            label = { Text("Video") },
            leadingIcon = { Icon(Icons.Filled.VideoCall, contentDescription = null) },
        )
        AssistChip(
            onClick = { photo.launch(providerUri(prepareCapture("jpg"))) },
            label = { Text("Camera") },
            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
        )
        AssistChip(
            onClick = { record.launch(providerUri(prepareCapture("mp4"))) },
            label = { Text("Record") },
            leadingIcon = { Icon(Icons.Filled.VideoCall, contentDescription = null) },
        )
    }
}