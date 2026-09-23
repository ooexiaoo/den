package com.den.app.data.repo

import android.content.Context
import com.den.app.data.db.AttachmentDao
import com.den.app.data.db.IdCount
import com.den.app.data.model.Attachment
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class AttachmentRepository(context: Context, private val dao: AttachmentDao) {

    val mediaDir: File = File(context.filesDir, "media").apply { mkdirs() }

    fun fileFor(attachment: Attachment): File = File(mediaDir, attachment.storedName)

    fun newStoredName(displayName: String?): String {
        val ext = displayName?.substringAfterLast('.', "")?.takeIf { it.length in 1..8 && it.all(Char::isLetterOrDigit) }
        return "${UUID.randomUUID()}${if (ext != null) ".$ext" else ""}"
    }

    fun observeForOwner(ownerType: String, ownerId: Long): Flow<List<Attachment>> =
        dao.observeForOwner(ownerType, ownerId)

    fun observeCountsByOwner(ownerType: String): Flow<List<IdCount>> =
        dao.observeCountsByOwner(ownerType)

    suspend fun listForOwner(ownerType: String, ownerId: Long): List<Attachment> =
        dao.listForOwner(ownerType, ownerId)

    suspend fun allForBackup(): List<Attachment> = dao.allForBackup()

    suspend fun add(attachment: Attachment): Long = dao.insert(attachment)

    suspend fun remove(attachment: Attachment) {
        fileFor(attachment).delete()
        dao.delete(attachment)
    }

    suspend fun deleteForOwner(ownerType: String, ownerId: Long) {
        listForOwner(ownerType, ownerId).forEach { fileFor(it).delete() }
        dao.deleteForOwner(ownerType, ownerId)
    }
}