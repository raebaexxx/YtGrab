package com.raebae.ytdl.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raebae.ytdl.R
import com.raebae.ytdl.data.SettingsRepository
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.data.ThemeMode
import com.raebae.ytdl.data.YtDlpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<SettingsState> = repo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _engineVersion = MutableStateFlow<String?>(null)
    val engineVersion: StateFlow<String?> = _engineVersion.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        refreshVersion()
    }

    fun refreshVersion() {
        viewModelScope.launch {
            _engineVersion.value = withContext(Dispatchers.IO) {
                runCatching { YtDlpEngine.versionName(getApplication()) }.getOrNull()
            }
        }
    }

    fun updateEngine() {
        if (_updating.value) return
        val app = getApplication<Application>()
        _updating.value = true
        viewModelScope.launch {
            try {
                val status = YtDlpEngine.updateYtDlp(app)
                val version = YtDlpEngine.versionName(app) ?: "?"
                _message.emit(
                    when (status) {
                        com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE ->
                            app.getString(R.string.updated, version)
                        com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ->
                            app.getString(R.string.up_to_date)
                        null -> app.getString(R.string.err_generic)
                    }
                )
                refreshVersion()
            } catch (e: Exception) {
                _message.emit(YtDlpEngine.friendlyError(e, app.getString(R.string.err_generic)))
            } finally {
                _updating.value = false
            }
        }
    }

    fun setSubFolder(value: String) = viewModelScope.launch { repo.setSubFolder(value) }
    fun setPlatformFolders(value: Boolean) = viewModelScope.launch { repo.setPlatformFolders(value) }
    fun setEmbedMetadata(value: Boolean) = viewModelScope.launch { repo.setEmbedMetadata(value) }
    fun setEmbedThumbnail(value: Boolean) = viewModelScope.launch { repo.setEmbedThumbnail(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repo.setDynamicColor(value) }
}
