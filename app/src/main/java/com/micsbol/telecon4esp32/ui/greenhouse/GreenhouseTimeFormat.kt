package com.micsbol.telecon4esp32.ui.greenhouse

internal fun formatDurationAgo(nowMs: Long, thenMs: Long): String {
    if (thenMs <= 0L) return "—"
    val seconds = ((nowMs - thenMs) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "${seconds}s"
        seconds < 3_600L -> "${seconds / 60L}m"
        seconds < 86_400L -> "${seconds / 3_600L}h"
        else -> "${seconds / 86_400L}d"
    }
}

internal fun formatDurationFromMinutes(minutes: Int): String {
    if (minutes < 0) return "—"
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        else -> "${minutes / 1_440}d"
    }
}
