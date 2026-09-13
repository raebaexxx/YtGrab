package com.raebae.ytdl.data

import android.content.Context
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object YtDlpEngine {

    private val initMutex = Mutex()
    private var initialized = false
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun ensureInit(appContext: Context) {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                runCatching { Aria2c.init(appContext) }
            }
            initialized = true
            engineScope.launch { autoUpdateIfOutdated(appContext) }
        }
    }

    /**
     * yt-dlp errors out on extractors when its release is older than ~90 days,
     * so silently update the binary if the bundled/installed one is stale.
     * On a fresh install there is no stored version at all -> update anyway.
     */
    private suspend fun autoUpdateIfOutdated(context: Context) {
        runCatching {
            val raw = YoutubeDL.getInstance().version(context)
            if (raw == null) {
                updateYtDlp(context)
                return
            }
            val m = Regex("(\\d{4})\\.(\\d{2})\\.(\\d{2})").find(raw) ?: return
            val (y, mo, d) = m.destructured
            val releaseDate = LocalDate.of(y.toInt(), mo.toInt(), d.toInt())
            if (ChronoUnit.DAYS.between(releaseDate, LocalDate.now()) > 60) {
                updateYtDlp(context)
            }
        }
    }

    /** yt-dlp refuses to extract when its release is >90 days old. */
    fun isOutdatedError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("older than 90 days") ||
            (msg.contains("yt-dlp version") && msg.contains("outdated")) ||
            (msg.contains("yt-dlp") && msg.contains("please update"))
    }

    suspend fun fetchMedia(appContext: Context, url: String): MediaResult {
        ensureInit(appContext)
        return withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("--socket-timeout", "30")
            }
            val response = YoutubeDL.getInstance().execute(request, null, null)
            parseMediaJson(url, response.out)
        }
    }

    internal fun parseMediaJson(originalUrl: String, rawOut: String): MediaResult {
        val start = rawOut.indexOf('{')
        require(start >= 0) { "empty response" }
        val json = JSONObject(rawOut.substring(start))

        val entries = json.optJSONArray("entries")
        val type = json.optString("_type")
        if ((type == "playlist" || type == "multi_video") && entries != null) {
            return MediaResult.Playlist(parsePlaylist(originalUrl, json, entries))
        }

        val duration = json.optDouble("duration", Double.NaN).let { if (it.isNaN()) null else it.toLong() }
        val formats = parseFormats(json)
        val options = FormatBuilder.buildOptions(formats, duration)
        if (options.isEmpty()) throw IllegalStateException("no downloadable formats")

        return MediaResult.Video(
            VideoUi(
                url = originalUrl,
                id = json.optString("id"),
                title = json.optString("title").ifBlank { "video" },
                uploader = json.optString("uploader", "").ifBlank {
                    json.optString("channel", "").ifBlank { null }
                },
                durationSec = duration,
                thumbnail = bestThumbnail(json),
                formats = options
            )
        )
    }

    private fun parsePlaylist(originalUrl: String, json: JSONObject, entries: JSONArray): PlaylistUi {
        val list = ArrayList<PlaylistEntryUi>(entries.length())
        for (i in 0 until entries.length()) {
            val e = entries.optJSONObject(i) ?: continue
            val id = e.optString("id", "")
            var url = e.optString("url", "")
            if (url.isBlank() || !url.startsWith("http")) {
                url = e.optString("webpage_url", "").ifBlank {
                    if (id.isNotBlank() && id.contains("http")) id
                    else "https://www.youtube.com/watch?v=$id"
                }
            }
            if (url.isBlank()) continue
            list += PlaylistEntryUi(
                id = id,
                url = url,
                title = e.optString("title", "").ifBlank { "video ${i + 1}" },
                durationSec = e.optDouble("duration", Double.NaN).let { if (it.isNaN()) null else it.toLong() }
            )
        }
        if (list.isEmpty()) throw IllegalStateException("empty playlist")

        return PlaylistUi(
            url = originalUrl,
            id = json.optString("id"),
            title = json.optString("title", "").ifBlank { "playlist" },
            uploader = json.optString("uploader", "").ifBlank {
                json.optString("channel", "").ifBlank { null }
            },
            thumbnail = bestThumbnail(json),
            entries = list
        )
    }

    private fun bestThumbnail(json: JSONObject): String? {
        json.optString("thumbnail", "").takeIf { it.isNotBlank() }?.let { return it }
        val thumbs = json.optJSONArray("thumbnails") ?: return null
        var best: JSONObject? = null
        var bestArea = -1L
        for (i in 0 until thumbs.length()) {
            val t = thumbs.optJSONObject(i) ?: continue
            val w = t.optLong("width", 0L)
            val h = t.optLong("height", 0L)
            val url = t.optString("url", "")
            if (url.isBlank()) continue
            val area = w * h
            if (area > bestArea) {
                bestArea = area
                best = t
            }
        }
        return best?.optString("url")
    }

    internal fun parseFormats(json: JSONObject): List<FormatBuilder.RawFormat> {
        val arr = json.optJSONArray("formats") ?: return emptyList()
        val result = ArrayList<FormatBuilder.RawFormat>(arr.length())
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val id = f.optString("format_id", "")
            if (id.isBlank()) continue
            result += FormatBuilder.RawFormat(
                formatId = id,
                ext = f.optString("ext", "").ifBlank { null },
                vcodec = f.optString("vcodec", "").ifBlank { null },
                acodec = f.optString("acodec", "").ifBlank { null },
                height = f.optInt("height", 0).takeIf { it > 0 },
                fps = f.optDouble("fps", Double.NaN).let { if (it.isNaN()) null else it.toFloat() },
                tbr = f.optDouble("tbr", Double.NaN).let { if (it.isNaN()) null else it.toFloat() },
                abr = f.optDouble("abr", Double.NaN).let { if (it.isNaN()) null else it.toFloat() },
                filesize = f.optLong("filesize", -1L).takeIf { it > 0 },
                filesizeApprox = f.optLong("filesize_approx", -1L).takeIf { it > 0 },
                protocol = f.optString("protocol", "").ifBlank { null }
            )
        }
        return result
    }

    suspend fun download(appContext: Context, task: DownloadRepository.DownloadTask): Unit {
        ensureInit(appContext)
        withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(task.url).apply {
                addOption("-f", task.formatSpec)
                addOption("-P", task.outputDir.absolutePath)
                addOption("-o", "${task.fileNameBase}.%(ext)s")
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--concurrent-fragments", "8")
                if (task.fastDownload) {
                    // aria2c external downloader; yt-dlp already passes -x16 -j16 -s16 by default
                    addOption("--downloader", "libaria2c.so")
                }
                if (task.embedMetadata) addOption("--embed-metadata")
                if (task.embedThumbnail) addOption("--embed-thumbnail")
                task.mergeExt?.let { addOption("--merge-output-format", it) }
            }
            YoutubeDL.getInstance().execute(request, task.id, null)
            Unit
        }
    }

    fun cancel(id: String): Boolean {
        val killed = YoutubeDL.getInstance().destroyProcessById(id)
        // The library kills child processes via "pstree | grep -P | xargs kill",
        // which does not exist on Android (toybox) — so an orphaned aria2c
        // spawned by yt-dlp keeps downloading after cancellation. Kill every
        // aria2c process whose command line mentions this download's marker.
        val marker = downloadMarkers.remove(id)
        if (marker != null) killAria2cByMarker(marker)
        return killed
    }

    private val downloadMarkers = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun registerDownloadMarker(id: String, outputDir: String) {
        downloadMarkers[id] = outputDir
    }

    fun releaseDownloadMarker(id: String) {
        downloadMarkers.remove(id)
    }

    private fun killAria2cByMarker(marker: String) {
        runCatching {
            val procDir = File("/proc")
            procDir.listFiles { f -> f.name.all { it.isDigit() } }?.forEach { pidDir ->
                val cmdline = File(pidDir, "cmdline").readText().replace('\u0000', ' ')
                if (cmdline.contains("aria2c") && cmdline.contains(marker)) {
                    Runtime.getRuntime().exec(arrayOf("kill", "-9", pidDir.name))
                }
            }
        }
    }

    suspend fun versionName(context: Context): String? {
        ensureInit(context)
        return withContext(Dispatchers.IO) { YoutubeDL.getInstance().versionName(context) }
    }

    suspend fun updateYtDlp(context: Context): YoutubeDL.UpdateStatus? {
        ensureInit(context)
        return withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
        }
    }

    fun friendlyError(e: Throwable, generic: String): String {
        val raw = e.message ?: e.toString()
        val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() } ?: return generic
        return firstLine
            .replace(Regex("^ERROR:\\s*"), "")
            .trim()
            .ifEmpty { generic }
    }
}
