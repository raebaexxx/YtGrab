package com.raebae.ytdl.ui.playlist

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raebae.ytdl.R
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.FormatBuilder
import com.raebae.ytdl.data.MediaResult
import com.raebae.ytdl.data.PlaylistUi
import com.raebae.ytdl.data.SettingsRepository
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.data.YtDlpEngine
import com.raebae.ytdl.util.PlaylistBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {

    data class Preset(
        val maxHeight: Int = 1080,
        val vcodecPrefix: String? = "avc1"
    )

    sealed interface UiState {
        data object Loading : UiState
        data class Error(val message: String) : UiState
        data class Ready(
            val playlist: PlaylistUi,
            val selectedIds: Set<String>,
            val preset: Preset
        ) : UiState
    }

    private val settingsRepo = SettingsRepository(app)

    val settings: StateFlow<SettingsState> = settingsRepo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val url = PlaylistBridge.pendingUrl
        PlaylistBridge.pendingUrl = null
        if (url.isNullOrBlank()) {
            _state.value = UiState.Error(app.getString(R.string.err_generic))
        } else {
            load(url)
        }
    }

    fun load(url: String) {
        val ctx = getApplication<Application>()
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = fetchMediaWithRetry(url)
                when (result) {
                    is MediaResult.Playlist -> _state.value = UiState.Ready(
                        result.playlist,
                        result.playlist.entries.map { it.id }.toSet(),
                        Preset()
                    )
                    is MediaResult.Video -> _state.value =
                        UiState.Error(ctx.getString(R.string.err_not_playlist))
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(mapError(e, ctx))
            }
        }
    }

    /** stale engine (older than 90 days): update yt-dlp and retry once */
    private suspend fun fetchMediaWithRetry(url: String): MediaResult {
        val app = getApplication<Application>()
        return try {
            YtDlpEngine.fetchMedia(app, url)
        } catch (e: Exception) {
            if (YtDlpEngine.isOutdatedError(e)) {
                YtDlpEngine.updateYtDlp(app)
                YtDlpEngine.fetchMedia(app, url)
            } else {
                throw e
            }
        }
    }

    fun toggle(id: String) {
        val ready = _state.value as? UiState.Ready ?: return
        val ids = ready.selectedIds.toMutableSet()
        if (!ids.add(id)) ids.remove(id)
        _state.value = ready.copy(selectedIds = ids)
    }

    fun selectAll() {
        val ready = _state.value as? UiState.Ready ?: return
        _state.value = ready.copy(selectedIds = ready.playlist.entries.map { it.id }.toSet())
    }

    fun deselectAll() {
        val ready = _state.value as? UiState.Ready ?: return
        _state.value = ready.copy(selectedIds = emptySet())
    }

    fun setMaxHeight(height: Int) {
        val ready = _state.value as? UiState.Ready ?: return
        _state.value = ready.copy(preset = ready.preset.copy(maxHeight = height))
    }

    fun setCodec(prefix: String?) {
        val ready = _state.value as? UiState.Ready ?: return
        _state.value = ready.copy(preset = ready.preset.copy(vcodecPrefix = prefix))
    }

    fun download() {
        val ready = _state.value as? UiState.Ready ?: return
        if (ready.selectedIds.isEmpty()) return
        val app = getApplication<Application>()
        val settingsValue = settings.value
        val preset = ready.preset
        val formatSpec = FormatBuilder.playlistTemplate(preset.maxHeight, preset.vcodecPrefix)
        val codecName = preset.vcodecPrefix?.let { codecName(it) }

        val tasks = ready.playlist.entries
            .filter { it.id in ready.selectedIds }
            .map { entry ->
                DownloadRepository.DownloadTask(
                    url = entry.url,
                    title = entry.title,
                    fileNameBase = FormatBuilder.sanitizeFileName(entry.title) + " [${entry.id}]",
                    formatSpec = formatSpec,
                    formatLabel = buildString {
                        append(preset.maxHeight).append("p")
                        if (codecName != null) append(" · ").append(codecName)
                    },
                    mergeExt = FormatBuilder.mergeExtFor(preset.vcodecPrefix, null),
                    outputDir = SettingsRepository.resolveOutputDir(app, settingsValue, entry.url),
                    sizeHint = null,
                    embedMetadata = settingsValue.embedMetadata,
                    embedThumbnail = settingsValue.embedThumbnail,
                    fastDownload = settingsValue.fastDownload
                )
            }
        DownloadRepository.enqueue(app, tasks)
    }

    private fun codecName(prefix: String): String = when {
        prefix.startsWith("avc") -> "H.264"
        prefix.startsWith("vp0") || prefix.startsWith("vp9") -> "VP9"
        prefix.startsWith("av01") -> "AV1"
        else -> prefix
    }

    private fun mapError(e: Throwable, ctx: Context): String = when (e.message) {
        "no downloadable formats" -> ctx.getString(R.string.err_no_formats)
        "empty playlist" -> ctx.getString(R.string.err_playlist_empty)
        else -> YtDlpEngine.friendlyError(e, ctx.getString(R.string.err_generic))
    }
}
