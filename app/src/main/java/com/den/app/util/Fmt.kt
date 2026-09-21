package com.den.app.util

import java.util.Locale

object Fmt {
    fun bytes(size: Long): String {
        if (size < 1024) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        return String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0)
    }

    fun shortTitle(title: String, max: Int = 120): String =
        if (title.length <= max) title else title.take(max - 1) + "…"

    val initials: (String) -> String = { t ->
        t.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }.take(2).joinToString("")
    }
}