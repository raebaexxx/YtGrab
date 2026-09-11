package com.raebae.ytdl

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.YtDlpEngine
import com.raebae.ytdl.service.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YtdlApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        DownloadRepository.attach(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { YtDlpEngine.ensureInit(this@YtdlApp) }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            DownloadService.CHANNEL_ID,
            getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
