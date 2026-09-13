package com.raebae.ytdl.data

import android.content.Context
import android.os.Environment
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val subFolder: String = "YtGrab",
    val platformFolders: Boolean = false,
    val fastDownload: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedThumbnail: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SUBFOLDER = stringPreferencesKey("subfolder")
        val PLATFORM_FOLDERS = booleanPreferencesKey("platform_folders")
        val FAST_DOWNLOAD = booleanPreferencesKey("fast_download")
        val EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        val EMBED_THUMBNAIL = booleanPreferencesKey("embed_thumbnail")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val state: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            subFolder = p[Keys.SUBFOLDER] ?: "YtGrab",
            platformFolders = p[Keys.PLATFORM_FOLDERS] ?: false,
            fastDownload = p[Keys.FAST_DOWNLOAD] ?: true,
            embedMetadata = p[Keys.EMBED_METADATA] ?: true,
            embedThumbnail = p[Keys.EMBED_THUMBNAIL] ?: false,
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: true
        )
    }

    suspend fun setSubFolder(value: String) {
        context.dataStore.edit { it[Keys.SUBFOLDER] = sanitizeSubFolder(value) }
    }

    suspend fun setPlatformFolders(value: Boolean) {
        context.dataStore.edit { it[Keys.PLATFORM_FOLDERS] = value }
    }

    suspend fun setFastDownload(value: Boolean) {
        context.dataStore.edit { it[Keys.FAST_DOWNLOAD] = value }
    }

    suspend fun setEmbedMetadata(value: Boolean) {
        context.dataStore.edit { it[Keys.EMBED_METADATA] = value }
    }

    suspend fun setEmbedThumbnail(value: Boolean) {
        context.dataStore.edit { it[Keys.EMBED_THUMBNAIL] = value }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = value.name }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    }

    companion object {

        /**
         * Keeps only safe path characters and blocks path traversal: the
         * resolved folder must stay inside public Downloads.
         */
        fun sanitizeSubFolder(value: String): String {
            var cleaned = value.trim().replace('\\', '/')
                .split('/')
                .map { segment -> segment.trim().replace(Regex("[^\\w\\s.\\-()\\[\\]]"), "").replace(Regex("\\.+"), ".") }
                .filter { it.isNotBlank() && it != "." }
                .joinToString("/")
            if (cleaned.isBlank()) cleaned = "YtGrab"
            return cleaned.take(64).trimEnd('/').ifBlank { "YtGrab" }
        }

        fun platformFolderName(url: String): String? = when {
            url.contains("youtu", ignoreCase = true) -> "YouTube"
            url.contains("tiktok", ignoreCase = true) -> "TikTok"
            else -> null
        }

        fun resolveOutputDir(context: Context, settings: SettingsState, url: String): File {
            var sub = sanitizeSubFolder(settings.subFolder)
            if (settings.platformFolders) {
                platformFolderName(url)?.let { sub = "$sub/$it" }
            }
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                sub
            )
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }
}
