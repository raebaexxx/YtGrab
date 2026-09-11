package com.yausername.ffmpeg

import android.content.Context
import java.io.File

/**
 * Slim local replacement for the upstream ffmpeg module.
 *
 * Our ffmpeg/ffprobe are fully static binaries (only bionic + libz), so unlike
 * the upstream termux build there is nothing to unpack: the old 35 MB
 * libffmpeg.zip.so with shared libs is simply not needed. We only keep the
 * packages dir alive because YoutubeDL references it in LD_LIBRARY_PATH.
 */
object FFmpeg {
    private var initialized = false

    @Synchronized
    fun init(appContext: Context) {
        if (initialized) return
        val ffmpegDir = File(
            File(File(appContext.noBackupFilesDir, "youtubedl-android"), "packages"),
            "ffmpeg"
        )
        val marker = File(ffmpegDir, ".ok")
        if (!marker.exists()) {
            // remove leftovers from the old termux-based module (upgrade path)
            ffmpegDir.deleteRecursively()
            ffmpegDir.mkdirs()
            marker.createNewFile()
        }
        initialized = true
    }

    @JvmStatic
    fun getInstance() = this
}
