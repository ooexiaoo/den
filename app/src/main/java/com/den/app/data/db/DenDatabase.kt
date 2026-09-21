package com.den.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.den.app.data.model.Attachment
import com.den.app.data.model.Label
import com.den.app.data.model.Note
import com.den.app.data.model.NoteLabelCrossRef
import com.den.app.data.model.Subtask
import com.den.app.data.model.Task
import com.den.app.data.model.TaskLabelCrossRef
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Task::class,
        Subtask::class,
        Note::class,
        Label::class,
        TaskLabelCrossRef::class,
        NoteLabelCrossRef::class,
        Attachment::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DenDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        private const val DB_NAME = "den.db"

        fun build(context: Context, passphrase: String): DenDatabase {
            // Load the SQLCipher native library (no-op if already loaded).
            System.loadLibrary("sqlcipher")
            val factory: SupportSQLiteOpenHelper.Factory =
                SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))
            return Room.databaseBuilder(context, DenDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .build()
        }
    }
}