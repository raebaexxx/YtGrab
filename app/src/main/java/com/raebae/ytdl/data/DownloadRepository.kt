package com.raebae.ytdl.data

import android.content.Context
import android.content.Intent
import com.raebae.ytdl.service.DownloadService
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        val thumbnail: String?,
        val sizeHint: Long?,
        val embedMetadata: Boolean = true,
        val embedThumbnail: Boolean = false,
        val fastDownload: Boolean = true,
        val status: Status = Status.QUEUED,
        val progress: Float = 0f,
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
        _tasks.update { it + newTasks }
        runCatching {
            val intent = Intent(context, DownloadService::class.java)
            context.startForegroundService(intent)
        }
        scope.launch { wake.send(Unit) }
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
        try {
            YtDlpEngine.download(task) { percent, eta, speed ->
                _tasks.update { list ->
                    list.map {
                        if (it.id == task.id) it.copy(progress = percent, etaSec = eta, speed = speed)
                        else it
                    }
                }
            }
            val ext = task.mergeExt ?: "mp4"
            val path = File(task.outputDir, "${task.fileNameBase}.$ext").absolutePath
            _tasks.update { list ->
                list.map {
                    if (it.id == task.id) it.copy(
                        status = Status.COMPLETED,
                        progress = 100f,
                        filePath = path
                    ) else it
                }
            }
        } catch (e: YoutubeDL.CanceledException) {
            _tasks.update { list ->
                list.map { if (it.id == task.id) it.copy(status = Status.CANCELED) else it }
            }
        } catch (e: Exception) {
            _tasks.update { list ->
                list.map {
                    if (it.id == task.id) it.copy(
                        status = Status.FAILED,
                        error = YtDlpEngine.friendlyError(e, "download failed")
                    ) else it
                }
            }
        } finally {
            scope.launch { wake.send(Unit) }
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
        scope.launch { wake.send(Unit) }
    }

    fun clearFinished() {
        _tasks.update { list ->
            list.filterNot { it.status in listOf(Status.COMPLETED, Status.FAILED, Status.CANCELED) }
        }
    }
}
