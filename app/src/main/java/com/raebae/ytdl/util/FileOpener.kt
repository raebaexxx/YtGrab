package com.raebae.ytdl.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.raebae.ytdl.R
import java.io.File

object FileOpener {

    fun open(context: Context, path: String?) {
        if (path == null) return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, R.string.file_open_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeFor(file.extension))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, R.string.file_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mimeFor(ext: String): String = when (ext.lowercase()) {
        "mp4", "m4v", "mov" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "opus" -> "audio/opus"
        "ogg" -> "audio/ogg"
        else -> "*/*"
    }
}
