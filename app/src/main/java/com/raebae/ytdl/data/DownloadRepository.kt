package com.raebae.ytdl.data

import android.content.Context
import android.content.Intent
import com.raebae.ytdl.R
import com.raebae.ytdl.service.DownloadService
import com.raebae.ytdl.util.FormatUtils
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object DownloadRepository {

    enum class Status { QUEUED, RUNNING, COMPLETED, FAILED, CANCELED }

    data class DownloadTask(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val title: String,
        val fileNameBase: String,
        val formatSpec: String,
        val formatLabel: String,
        val mergeExt: String?,
        val outputDir: File,
        val sizeHint: Long?,
        val embedMetadata: Boolean = true,
        val embedThumbnail: Boolean = false,
        val fastDownload: Boolean = true,
        val status: Status = Status.QUEUED,
        val progress: Float = 0f,
        val bytesDone: Long? = null,
        val etaSec: Long? = null,
        val speed: String? = null,
        val error: String? = null,
        val filePath: String? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val workerStarted = AtomicBoolean(false)
    private var appContext: Context? = null

    fun attach(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        if (workerStarted.compareAndSet(false, true)) {
            scope.launch { worker() }
        }
    }

    fun enqueue(context: Context, newTasks: List<DownloadTask>) {
        if (newTasks.isEmpty()) return
        attach(context)
        val accepted = _tasks.updateAndGet { list ->
            // skip exact duplicates (same video+format) that are still pending/running
            val activeKeys = list
                .filter { it.status == Status.QUEUED || it.status == Status.RUNNING }
                .map { it.url to it.formatSpec }
                .toSet()
            val fresh = newTasks.filter { (it.url to it.formatSpec) !in activeKeys }
            if (fresh.isEmpty()) list else list + fresh
        }
        if (accepted.none { it.status == Status.QUEUED || it.status == Status.RUNNING }) {
            // nothing new was queued (all duplicates) — do not resurrect the service
            return
        }
        startService(context)
        scope.launch { wake.send(Unit) }
    }

    /** The service stops itself when the queue empties, so any path that
     *  re-queues a task must bring it (and its foreground notification) back. */
    private fun startService(context: Context) {
        runCatching {
            val intent = Intent(context, DownloadService::class.java)
            context.startForegroundService(intent)
        }
    }

    private suspend fun worker() {
        for (unit in wake) {
            while (true) {
                val next = _tasks.value.firstOrNull { it.status == Status.QUEUED } ?: break
                runTask(next)
            }
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == task.id) it.copy(status = Status.RUNNING) else it } }
        withContext(Dispatchers.IO) { cleanStaleArtifacts(task) }
        YtDlpEngine.registerDownloadMarker(task.id, task.outputDir.absolutePath)
        // the library's stdout parser crashes on aria2c console lines, so track
        // progress by polling the growing .part files on disk instead
        val poller = scope.launch {
            pollProgress(task.id, task.outputDir, task.fileNameBase, task.sizeHint)
        }
        try {
            var attempt = 0
            while (true) {
                try {
                    appContext?.let { YtDlpEngine.download(it, task) }
                        ?: throw IllegalStateException("repository not attached")
                    break
                } catch (e: YoutubeDL.CanceledException) {
                    throw e
                } catch (e: Exception) {
                    // stale engine: update yt-dlp and retry once
                    if (attempt == 0 && YtDlpEngine.isOutdatedError(e)) {
                        attempt++
                        runCatching { appContext?.let { YtDlpEngine.updateYtDlp(it) } }
                        continue
                    }
                    throw e
                }
            }
            val ext = task.mergeExt ?: "mp4"
            val path = File(task.outputDir, "${task.fileNameBase}.$ext").absolutePath
            finalizeArtifacts(task, succeeded = true)
            _tasks.update { list ->
                list.map {
                    if (it.id == task.id) it.copy(
                        status = Status.COMPLETED,
                        progress = 100f,
                        bytesDone = task.sizeHint,
                        filePath = path
                    ) else it
                }
            }
        } catch (e: YoutubeDL.CanceledException) {
            finalizeArtifacts(task, succeeded = false)
            _tasks.update { list ->
                list.map { if (it.id == task.id) it.copy(status = Status.CANCELED) else it }
            }
        } catch (e: Exception) {
            finalizeArtifacts(task, succeeded = false)
            val msg = if ((e.message ?: "").contains("Permission denied")) {
                appContext?.getString(R.string.err_permission_denied)
                    ?: YtDlpEngine.friendlyError(e, "download failed")
            } else {
                YtDlpEngine.friendlyError(e, "download failed")
            }
            _tasks.update { list ->
                list.map {
                    if (it.id == task.id) it.copy(
                        status = Status.FAILED,
                        error = msg
                    ) else it
                }
            }
        } finally {
            YtDlpEngine.releaseDownloadMarker(task.id)
            poller.cancel()
            scope.launch { wake.send(Unit) }
        }
    }

    /**
     * Files left over from previous attempts/downloads (.part, .meta, .temp.*,
     * the final file itself) can belong to a previous app install — Android's
     * scoped storage (FUSE) then denies us write access when yt-dlp or ffmpeg
     * tries to replace them. Delete everything matching this task's base name
     * before downloading so ffmpeg never has to overwrite a foreign file.
     *
     * The previously downloaded final file is kept as a `.bak` until the new
     * attempt succeeds, so a failed retry does not destroy the user's copy.
     */
    private fun cleanStaleArtifacts(task: DownloadTask) {
        val finalName = "${task.fileNameBase}.${task.mergeExt ?: "mp4"}"
        val bakName = "$finalName.bak"
        val files = task.outputDir.listFiles() ?: return
        for (f in files) {
            if (!f.name.startsWith(task.fileNameBase)) continue
            if (f.name == finalName || f.name == bakName) continue
            runCatching { f.delete() }
        }
        val final = File(task.outputDir, finalName)
        if (final.exists()) {
            val bak = File(task.outputDir, "$finalName.bak")
            runCatching { bak.delete() }
            val renamed = runCatching { final.renameTo(bak) }.getOrDefault(false)
            if (!renamed) runCatching { final.delete() }
        }
    }

    /** After a finished attempt: drop the backup on success, restore it otherwise. */
    private fun finalizeArtifacts(task: DownloadTask, succeeded: Boolean) {
        val finalName = "${task.fileNameBase}.${task.mergeExt ?: "mp4"}"
        val bak = File(task.outputDir, "$finalName.bak")
        if (!bak.exists()) return
        runCatching {
            if (succeeded) {
                bak.delete()
            } else {
                val final = File(task.outputDir, finalName)
                if (final.exists()) final.delete()
                bak.renameTo(final)
            }
        }
    }

    private suspend fun pollProgress(id: String, dir: File, fileNameBase: String, sizeHint: Long?) {
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()
        var lastBps = 0.0
        var lastSpeed: String? = null
        while (true) {
            delay(700)
            val bytes = withContext(Dispatchers.IO) {
                dir.listFiles()
                    ?.filter { it.name.startsWith(fileNameBase) && it.name.endsWith(".part") }
                    ?.sumOf { it.length() } ?: 0L
            }
            val now = System.currentTimeMillis()
            val dt = now - lastTime
            if (bytes > lastBytes && dt > 0) {
                lastBps = (bytes - lastBytes) * 1000.0 / dt
                lastSpeed = FormatUtils.formatSpeed(lastBps)
            }
            lastBytes = bytes
            lastTime = now
            val percent = sizeHint?.takeIf { it > 0 }
                ?.let { (bytes * 100.0 / it).toFloat().coerceIn(0f, 99f) } ?: 0f
            val eta = sizeHint?.takeIf { it > 0 && lastBps > 0 }
                ?.let { ((it - bytes) / lastBps).toLong().takeIf { s -> s > 0 } }
            _tasks.update { list ->
                list.map {
                    if (it.id == id) it.copy(
                        progress = percent,
                        bytesDone = bytes,
                        speed = lastSpeed,
                        etaSec = eta
                    ) else it
                }
            }
        }
    }

    fun cancel(id: String) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        when (task.status) {
            Status.RUNNING -> YtDlpEngine.cancel(id)
            Status.QUEUED -> _tasks.update { list ->
                list.map { if (it.id == id) it.copy(status = Status.CANCELED) else it }
            }
            else -> Unit
        }
    }

    fun retry(id: String) {
        _tasks.update { list ->
            list.map {
                if (it.id == id && it.status in listOf(Status.FAILED, Status.CANCELED)) {
                    it.copy(
                        status = Status.QUEUED,
                        progress = 0f,
                        etaSec = null,
                        speed = null,
                        error = null,
                        filePath = null
                    )
                } else it
            }
        }
        appContext?.let { startService(it) }
        scope.launch { wake.send(Unit) }
    }

    fun clearFinished() {
        _tasks.update { list ->
            list.filterNot { it.status in listOf(Status.COMPLETED, Status.FAILED, Status.CANCELED) }
        }
    }
}
