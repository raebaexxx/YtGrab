package com.raebae.ytdl.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.raebae.ytdl.MainActivity
import com.raebae.ytdl.R
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.DownloadRepository.Status
import com.raebae.ytdl.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground(buildNotification(emptyList()))
        scope.launch {
            DownloadRepository.tasks.collect { tasks ->
                val active = tasks.firstOrNull { it.status == Status.RUNNING }
                    ?: tasks.firstOrNull { it.status == Status.QUEUED }
                if (active == null) {
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notify(buildNotification(tasks))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_ID)?.let { DownloadRepository.cancel(it) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun notify(notification: Notification) {
        getSystemService(android.app.NotificationManager::class.java).notify(NOTIF_ID, notification)
    }

    private fun buildNotification(tasks: List<DownloadRepository.DownloadTask>): Notification {
        val running = tasks.firstOrNull { it.status == Status.RUNNING }
        val queuedCount = tasks.count { it.status == Status.QUEUED }
        val active = running ?: tasks.firstOrNull { it.status == Status.QUEUED }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setContentTitle(getString(R.string.notif_downloading))
            .setContentIntent(contentIntent())

        if (active == null) {
            return builder.build()
        }

        when (active.status) {
            Status.RUNNING -> {
                val parts = mutableListOf<String>()
                if (active.progress > 0) {
                    parts += "${active.progress.toInt()}%"
                    FormatUtils.formatEta(active.etaSec).takeIf { it.isNotEmpty() }?.let { parts += it }
                } else {
                    active.bytesDone?.takeIf { it > 0 }?.let { parts += FormatUtils.formatBytes(it) }
                }
                active.speed?.let { parts += it }
                builder.setContentText(parts.joinToString(" · "))
                builder.setProgress(100, active.progress.toInt(), active.progress <= 0f)
            }
            else -> {
                builder.setContentText(getString(R.string.status_queued))
                builder.setProgress(100, 0, true)
            }
        }
        if (queuedCount > 0) {
            builder.setSubText(getString(R.string.notif_queue_n, queuedCount))
        }
        builder.addAction(
            0,
            getString(R.string.cancel),
            cancelPendingIntent(active.id)
        )
        return builder.build()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun cancelPendingIntent(id: String): PendingIntent =
        PendingIntent.getService(
            this,
            id.hashCode(),
            Intent(this, DownloadService::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        const val CHANNEL_ID = "downloads"
        const val ACTION_CANCEL = "com.raebae.ytdl.action.CANCEL"
        const val EXTRA_ID = "id"
        private const val NOTIF_ID = 1001
    }
}
