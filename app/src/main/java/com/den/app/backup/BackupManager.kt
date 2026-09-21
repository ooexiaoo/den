package com.den.app.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.den.app.crypto.AppCrypto
import com.den.app.data.db.DenDatabase
import com.den.app.data.model.Attachment
import com.den.app.data.model.Label
import com.den.app.data.model.Note
import com.den.app.data.model.NoteLabelCrossRef
import com.den.app.data.model.Subtask
import com.den.app.data.model.Task
import com.den.app.data.model.TaskLabelCrossRef
import com.den.app.data.repo.AttachmentRepository
import com.den.app.settings.Settings
import com.den.app.settings.SettingsStore
import com.den.app.util.Dates
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    context: Context,
    private val db: DenDatabase,
    private val attachments: AttachmentRepository,
    private val settings: SettingsStore,
    private val crypto: AppCrypto,
) {

    private val appContext = context.applicationContext
    val backupDir: File = File(appContext.filesDir, "backups").apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun currentSettings(): Settings = settings.settings.firstOrNull() ?: Settings()

    fun listBackups(): List<File> = files()

    suspend fun isDue(cfg: Settings? = null): Boolean {
        val settings = cfg ?: currentSettings()
        return settings.backupEnabled && isAutoBackupDue(settings)
    }

    private suspend fun isAutoBackupDue(cfg: Settings): Boolean {
        if (!cfg.backupEnabled) return false
        val now = System.currentTimeMillis()
        val last = cfg.lastBackupAt
        return when (cfg.backupFrequency) {
            1 -> last <= 0 || now - last >= 7 * 86_400_000L
            else -> {
                if (last <= 0) return true
                val target = Dates.startOfDay(now) + cfg.backupHour * 3_600_000L
                last < target
            }
        }
    }

    suspend fun createAutoBackup(force: Boolean = false): Result<File> {
        return runCatching {
            val cfg = currentSettings()
            if (!force) {
                check(isAutoBackupDue(cfg)) { "not_due" }
            } else {
                check(cfg.backupEnabled) { "disabled" }
            }
            val file = doCreateBackup(cfg, includeMedia = cfg.backupIncludeMedia)
            settings.setLastBackupAt(System.currentTimeMillis())
            prune(cfg.backupKeepCount)
            file
        }
    }

    suspend fun exportToUri(uri: Uri, includeMedia: Boolean): Result<Int> {
        return runCatching {
            val cfg = currentSettings()
            val out: OutputStream = appContext.contentResolver.openOutputStream(uri)
                ?: error("Cannot open destination")
            val count = out.use { writeBackup(cfg, includeMedia, it) }
            settings.setLastBackupAt(System.currentTimeMillis())
            count
        }
    }

    suspend fun restoreFromUri(uri: Uri, passphraseOverride: String?): Result<Long> {
        return runCatching {
            val ins: InputStream = appContext.contentResolver.openInputStream(uri)
                ?: error("Cannot open backup file")
            val cfg = currentSettings()
            val pass = passphraseOverride?.takeIf { it.isNotEmpty() } ?: cfg.backupPassphrase
            val plain = if (cfg.backupEncryption == "passphrase" && !pass.isNullOrEmpty()) {
                crypto.passphraseDecryptSource(pass, ins)
            } else {
                crypto.streamDecryptSource(ins)
            }
            val count = restore(plain, includeMedia = true)
            settings.setLastBackupAt(System.currentTimeMillis())
            count
        }
    }

    private suspend fun restore(ins: InputStream, includeMedia: Boolean): Long {
        val zip = ZipInputStream(BufferedInputStream(ins))
        var dataJson: String? = null
        var totalBytes = 0L
        try {
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == "data.json" -> {
                        val bytes = zip.readBytes()
                        dataJson = bytes.decodeToString()
                        totalBytes += bytes.size
                    }
                    entry.name.startsWith("media/") && includeMedia -> {
                        val name = entry.name.removePrefix("media/")
                        val bytes = zip.readBytes()
                        totalBytes += bytes.size
                        attachments.mediaDir.mkdirs()
                        File(attachments.mediaDir, name).writeBytes(bytes)
                    }
                    else -> zip.readBytes()
                }
                zip.closeEntry()
            }
        } finally {
            zip.close()
        }
        val payload = json.decodeFromString<BackupPayload>(dataJson ?: error("Backup missing data.json"))

        db.withTransaction {
            db.taskDao().clearAll()
            db.subtaskDao().clearAll()
            db.noteDao().clearAll()
            db.labelDao().clearAll()
            db.attachmentDao().clearAll()
            db.labelDao().clearTaskCrossRefs()
            db.labelDao().clearNoteCrossRefs()

            db.taskDao().insertAll(payload.tasks.map { it.toEntity() })
            db.subtaskDao().insertAll(payload.subtasks.map { it.toEntity() })
            db.noteDao().insertAll(payload.notes.map { it.toEntity() })
            db.labelDao().insertAll(payload.labels.map { it.toEntity() })
            db.labelDao().insertTaskCrossRefs(payload.taskLabels.map { TaskLabelCrossRef(it.taskId, it.labelId) })
            db.labelDao().insertNoteCrossRefs(payload.noteLabels.map { NoteLabelCrossRef(it.noteId, it.labelId) })
            db.attachmentDao().insertAll(payload.attachments.map { it.toEntity() })
        }
        return totalBytes
    }

    private suspend fun doCreateBackup(cfg: Settings, includeMedia: Boolean): File {
        val name = "den_${System.currentTimeMillis()}.backup"
        val target = File(backupDir, name)
        val tmp = File(appContext.cacheDir, "backup_pending.backup")
        try {
            FileOutputStream(tmp).buffered().use { writeBackup(cfg, includeMedia, it) }
            target.outputStream().use { out -> tmp.inputStream().use { it.copyTo(out) } }
        } finally {
            tmp.delete()
        }
        return target
    }

    private suspend fun writeBackup(cfg: Settings, includeMedia: Boolean, out: OutputStream): Int {
        val payload = buildPayload()
        val pass = cfg.backupPassphrase.takeIf { it.isNotEmpty() }
        val cipherOut = if (cfg.backupEncryption == "passphrase" && pass != null) {
            crypto.passphraseEncryptSink(pass, out)
        } else {
            crypto.streamEncryptSink(out)
        }
        var total = 0
        ZipOutputStream(cipherOut).use { zip ->
            zip.setLevel(9)
            val dataBytes = json.encodeToString(BackupPayload.serializer(), payload).toByteArray()
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(dataBytes)
            zip.closeEntry()
            total += dataBytes.size
            if (includeMedia) {
                for (a in payload.attachments) {
                    val file = attachments.fileFor(a.toEntity())
                    if (!file.exists()) continue
                    val bytes = file.readBytes()
                    zip.putNextEntry(ZipEntry("media/${a.storedName}"))
                    zip.write(bytes)
                    zip.closeEntry()
                    total += bytes.size
                }
            }
        }
        cipherOut.close()
        return total
    }

    private suspend fun buildPayload(): BackupPayload {
        val tasks = db.taskDao().allForBackup()
        val subtasks = db.subtaskDao().listAll()
        val notes = db.noteDao().allForBackup()
        val labels = db.labelDao().allForBackup()
        val taskLabels = db.labelDao().allTaskCrossRefs()
        val noteLabels = db.labelDao().allNoteCrossRefs()
        val attachments = db.attachmentDao().allForBackup()
        return BackupPayload(
            createdAt = System.currentTimeMillis(),
            tasks = tasks.map { it.toDto() },
            subtasks = subtasks.map { it.toDto() },
            notes = notes.map { it.toDto() },
            labels = labels.map { it.toDto() },
            taskLabels = taskLabels.map { TaskLabelDto(it.taskId, it.labelId) },
            noteLabels = noteLabels.map { NoteLabelDto(it.noteId, it.labelId) },
            attachments = attachments.map { it.toDto() },
        )
    }

    fun files(): List<File> = backupDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun prune(keep: Int) {
        files().drop(keep.coerceAtLeast(1)).forEach { it.delete() }
    }
}

private fun TaskDto.toEntity(): Task = Task(
    id = id, title = title, notes = notes, dueAt = dueAt, reminderAt = reminderAt,
    completed = completed, completeRating = completeRating, completeReflection = completeReflection,
    completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt, archived = archived,
    pinned = pinned, colorIndex = colorIndex, priority = priority, sortOrder = sortOrder,
)

private fun Task.toDto(): TaskDto = TaskDto(
    id = id, title = title, notes = notes, dueAt = dueAt, reminderAt = reminderAt,
    completed = completed, completeRating = completeRating, completeReflection = completeReflection,
    completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt, archived = archived,
    pinned = pinned, colorIndex = colorIndex, priority = priority, sortOrder = sortOrder,
)

private fun SubtaskDto.toEntity(): Subtask = Subtask(id, taskId, title, done, sortOrder)

private fun Subtask.toDto(): SubtaskDto = SubtaskDto(id, taskId, title, done, sortOrder)

private fun NoteDto.toEntity(): Note = Note(
    id = id, title = title, body = body, pinned = pinned, archived = archived,
    colorIndex = colorIndex, createdAt = createdAt, updatedAt = updatedAt,
)

private fun Note.toDto(): NoteDto = NoteDto(
    id = id, title = title, body = body, pinned = pinned, archived = archived,
    colorIndex = colorIndex, createdAt = createdAt, updatedAt = updatedAt,
)

private fun Label.toDto(): LabelDto = LabelDto(id, name, colorIndex, createdAt)

private fun LabelDto.toEntity(): Label = Label(id, name, colorIndex, createdAt)

private fun AttachmentDto.toEntity(): Attachment = Attachment(
    id = id, ownerType = ownerType, ownerId = ownerId, purpose = purpose, kind = kind,
    storedName = storedName, displayName = displayName, mime = mime, size = size, createdAt = createdAt,
)

private fun Attachment.toDto(): AttachmentDto = AttachmentDto(
    id = id, ownerType = ownerType, ownerId = ownerId, purpose = purpose, kind = kind,
    storedName = storedName, displayName = displayName, mime = mime, size = size, createdAt = createdAt,
)