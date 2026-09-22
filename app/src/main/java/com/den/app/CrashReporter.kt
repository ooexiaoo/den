package com.den.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures uncaught crashes so they can be surfaced on the next launch,
 * making it easy to copy a crash stack without adb/logcat.
 */
object CrashReporter {

    private const val PREFS = "den_crash"
    private const val KEY_LAST = "last_crash"
    private const val FILE_NAME = "den_last_crash.txt"
    private const val MAX_LEN = 16 * 1024

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                save(app, format(thread, throwable))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Persist a crash that was caught explicitly (e.g. startup init) so nothing crash-loops. */
    fun capture(context: Context, thread: Thread, throwable: Throwable) {
        try {
            save(context, format(thread, throwable))
        } catch (_: Throwable) {
        }
    }

    fun consumeLast(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getString(KEY_LAST, null) ?: return null
        prefs.edit().remove(KEY_LAST).commit()
        return last
    }

    private fun save(context: Context, text: String) {
        // commit() (not apply()) so the write lands before the process dies.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST, text)
            .commit()
        try {
            val dir = context.getExternalFilesDir(null) ?: return
            File(dir, FILE_NAME).writeText(text)
        } catch (_: Throwable) {
        }
    }

    private fun format(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        var trace = sw.toString()
        if (trace.length > MAX_LEN) {
            trace = trace.take(MAX_LEN) + "\n...(truncated)"
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("=== Den crash @ $stamp ===")
            appendLine("thread: ${thread.name}")
            appendLine("model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("abi: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("--- stack ---")
            append(trace)
        }
    }
}