package com.raebae.ytdl.util

import kotlinx.coroutines.flow.MutableStateFlow

/** Bridge for URLs received from share/open intents. */
object UrlBridge {
    val pendingUrl = MutableStateFlow<String?>(null)

    fun post(url: String) {
        pendingUrl.value = url
    }

    fun consume(): String? {
        val v = pendingUrl.value
        pendingUrl.value = null
        return v
    }
}

/** Handoff of the playlist URL to open the playlist screen. */
object PlaylistBridge {
    var pendingUrl: String? = null
}

object UrlUtils {
    private val urlRegex = Regex("(https?://\\S+)")

    /** Extracts the first http(s) URL from arbitrary shared text. */
    fun extractUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("http")) return trimmed.split(Regex("\\s")).first()
        return urlRegex.find(text)?.groupValues?.get(1)
    }

    /** If a watch URL contains &list=..., builds the pure playlist URL. */
    fun playlistUrlOf(url: String): String? {
        val listId = Regex("[?&]list=([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)
            ?: return null
        return "https://www.youtube.com/playlist?list=$listId"
    }
}

object FormatUtils {
    fun formatSpeed(bps: Double): String {
        val kb = bps / 1024.0
        if (kb < 1024) return "%.0f KB/s".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB/s".format(mb)
        return "%.2f GB/s".format(mb / 1024.0)
    }

    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes <= 0) return "—"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.0f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    fun formatDuration(sec: Long?): String {
        if (sec == null || sec <= 0) return "—"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun formatEta(sec: Long?): String {
        if (sec == null || sec <= 0) return ""
        return formatDuration(sec)
    }
}
