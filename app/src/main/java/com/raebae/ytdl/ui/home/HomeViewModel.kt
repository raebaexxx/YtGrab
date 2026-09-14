package com.raebae.ytdl.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raebae.ytdl.R
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.FormatBuilder
import com.raebae.ytdl.data.MediaResult
import com.raebae.ytdl.data.SettingsRepository
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.data.VideoFormatOption
import com.raebae.ytdl.data.VideoUi
import com.raebae.ytdl.data.YtDlpEngine
import com.raebae.ytdl.util.PlaylistBridge
import com.raebae.ytdl.util.UrlBridge
import com.raebae.ytdl.util.UrlUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Error(val message: String) : UiState
        data class Ready(val video: VideoUi, val selected: VideoFormatOption?) : UiState
    }

    private val settingsRepo = SettingsRepository(app)

    val settings: StateFlow<SettingsState> = settingsRepo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _openPlaylist = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openPlaylist: SharedFlow<String> = _openPlaylist.asSharedFlow()

    private var lastUrl: String? = null

    init {
        viewModelScope.launch {
            UrlBridge.pendingUrl.collect { shared ->
                if (shared != null) {
                    UrlBridge.consume()
                    onSharedUrl(shared)
                }
            }
        }
    }

    fun onUrlChanged(value: String) {
        _url.value = value
    }

    fun fetch() {
        fetchUrl(_url.value)
    }

    private fun onSharedUrl(url: String) {
        _url.value = url
        fetchUrl(url)
    }

    private fun fetchUrl(raw: String) {
        val ctx = getApplication<Application>()
        val url = raw.trim()
        if (url.isEmpty()) {
            _state.value = UiState.Error(ctx.getString(R.string.err_empty_url))
            return
        }
        lastUrl = url
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                when (val result = fetchMediaWithRetry(url)) {
                    is MediaResult.Video ->
                        _state.value = UiState.Ready(result.video, result.video.formats.firstOrNull())
                    is MediaResult.Playlist ->
                        _openPlaylist.emit(result.playlist.url)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
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

    fun selectFormat(option: VideoFormatOption) {
        val current = _state.value as? UiState.Ready ?: return
        _state.value = current.copy(selected = option)
    }

    fun playlistUrl(): String? = lastUrl?.let { UrlUtils.playlistUrlOf(it) }

    fun download() {
        val ready = _state.value as? UiState.Ready ?: return
        val option = ready.selected ?: return
        val app = getApplication<Application>()
        val settingsValue = settings.value
        val dir = SettingsRepository.resolveOutputDir(app, settingsValue, ready.video.url)
        val task = DownloadRepository.DownloadTask(
            url = ready.video.url,
            title = ready.video.title,
            fileNameBase = FormatBuilder.sanitizeFileName(ready.video.title) + " [${ready.video.id}]",
            formatSpec = option.formatSpec,
            formatLabel = formatLabel(option),
            mergeExt = FormatBuilder.mergeExtFor(option.vcodec, option.ext),
            outputDir = dir,
            sizeHint = option.sizeBytes,
            embedMetadata = settingsValue.embedMetadata,
            embedThumbnail = settingsValue.embedThumbnail,
            fastDownload = settingsValue.fastDownload
        )
        DownloadRepository.enqueue(app, listOf(task))
    }

    private fun formatLabel(option: VideoFormatOption): String {
        val fps = if (option.fps > 30) option.fps.toString() else ""
        val codec = option.vcodecLabel
        val audio = option.audioCodecLabel
        return buildString {
            append(option.height).append("p").append(fps)
            append(" · ").append(codec)
            if (audio != null) append(" + ").append(audio)
        }
    }

    private fun mapError(e: Throwable, ctx: Context): String = when (e.message) {
        "no downloadable formats" -> ctx.getString(R.string.err_no_formats)
        "empty playlist" -> ctx.getString(R.string.err_playlist_empty)
        else -> YtDlpEngine.friendlyError(e, ctx.getString(R.string.err_generic))
    }
}
