package com.den.app.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.den.app.data.model.AttachKinds
import com.den.app.data.model.Attachment
import com.den.app.data.repo.AttachmentRepository
import java.io.File
import java.io.FileOutputStream

class MediaImporter(
    private val context: Context,
    private val attachmentRepo: AttachmentRepository,
) {

    suspend fun importFromUri(
        uri: Uri,
        ownerType: String,
        ownerId: Long,
        purpose: String,
    ): Attachment? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: ""
        val display = displayName(uri)
        val stored = attachmentRepo.newStoredName(display ?: extensionFor(mime))
        val target = File(attachmentRepo.mediaDir, stored)
        var copied = 0L
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                }
            }
        } ?: return null
        if (copied <= 0) {
            target.delete()
            return null
        }
        val attachment = Attachment(
            ownerType = ownerType,
            ownerId = ownerId,
            purpose = purpose,
            kind = kindForMime(mime),
            storedName = stored,
            displayName = display ?: stored.substringBeforeLast('.'),
            mime = mime.ifEmpty { "application/octet-stream" },
            size = copied,
            createdAt = System.currentTimeMillis(),
        )
        attachmentRepo.add(attachment)
        return attachment
    }

    suspend fun importFromFile(
        file: File,
        mime: String,
        displayName: String,
        ownerType: String,
        ownerId: Long,
        purpose: String,
    ): Attachment? {
        if (!file.exists() || file.length() <= 0) return null
        val stored = attachmentRepo.newStoredName(displayName)
        val target = File(attachmentRepo.mediaDir, stored)
        file.copyTo(target, overwrite = true)
        val attachment = Attachment(
            ownerType = ownerType,
            ownerId = ownerId,
            purpose = purpose,
            kind = kindForMime(mime),
            storedName = stored,
            displayName = displayName,
            mime = mime.ifEmpty { "application/octet-stream" },
            size = target.length(),
            createdAt = System.currentTimeMillis(),
        )
        attachmentRepo.add(attachment)
        return attachment
    }

    private fun displayName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun extensionFor(mime: String): String? = when {
        mime.startsWith("image/") -> ".jpg"
        mime.startsWith("video/") -> ".mp4"
        mime.startsWith("audio/") -> ".m4a"
        else -> null
    }

    private fun kindForMime(mime: String): String = when {
        mime.startsWith("image/") -> AttachKinds.IMAGE
        mime.startsWith("video/") -> AttachKinds.VIDEO
        mime.startsWith("audio/") -> AttachKinds.AUDIO
        else -> AttachKinds.FILE
    }
}