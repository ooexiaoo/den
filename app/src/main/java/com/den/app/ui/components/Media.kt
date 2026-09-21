package com.den.app.ui.components

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.den.app.data.model.AttachKinds
import com.den.app.data.model.Attachment
import java.io.File

fun decodeThumb(file: File, targetSize: Int = 512): ImageBitmap? {
    if (!file.exists()) return null
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(file)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(
                    maxOf(1, minOf(info.size.width, info.size.height) / targetSize)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } else {
            val opts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = 1
            }
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxSide > 0) {
                var sample = 1
                while (maxSide / sample > targetSize * 2) sample *= 2
                opts.inSampleSize = sample
            }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AttachedMediaGrid(
    attachments: List<Attachment>,
    fileFor: (Attachment) -> File,
    onDelete: (Attachment) -> Unit,
) {
    if (attachments.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.chunked(3).forEach { rowAttachments ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowAttachments.forEach { attachment ->
                    Box(modifier = Modifier.weight(1f)) {
                        MediaTile(attachment, fileFor, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    attachment: Attachment,
    fileFor: (Attachment) -> File,
    onDelete: (Attachment) -> Unit,
) {
    val file = fileFor(attachment)
    var fullScreen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.aspectRatio(1f)) {
        Surface(
            modifier = Modifier.fillMaxSize().clickable { fullScreen = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (attachment.kind == AttachKinds.IMAGE) {
                    ThumbImage(file, Modifier.fillMaxSize())
                } else if (attachment.kind == AttachKinds.VIDEO) {
                    ThumbImage(file, Modifier.fillMaxSize())
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = attachment.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        IconButton(
            onClick = { onDelete(attachment) },
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
    if (fullScreen) {
        MediaViewer(attachment, file) { fullScreen = false }
    }
}

@Composable
fun ThumbImage(file: File, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = decodeThumb(file)
    }
    val bm = bitmap
    if (bm != null) {
        Image(
            bitmap = bm,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("media", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MediaViewer(attachment: Attachment, file: File, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            when (attachment.kind) {
                AttachKinds.IMAGE -> {
                    val bitmap by produceState<ImageBitmap?>(null, file) { value = decodeThumb(file, 1920) }
                    bitmap?.let {
                        Image(bitmap = it, contentDescription = attachment.displayName, modifier = Modifier.fillMaxWidth())
                    }
                }
                AttachKinds.VIDEO -> VideoPlayer(file)
                else -> Text(attachment.displayName, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun VideoPlayer(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                keepScreenOn = true
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        modifier = modifier,
    )
}