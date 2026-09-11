package com.raebae.ytdl.data

data class VideoFormatOption(
    val formatSpec: String,
    val height: Int,
    val fps: Int,
    val ext: String,
    val vcodec: String,
    val vcodecLabel: String,
    val audioSpec: String?,
    val audioCodecLabel: String?,
    val sizeBytes: Long?,
    val isProgressive: Boolean
)

data class VideoUi(
    val url: String,
    val id: String,
    val title: String,
    val uploader: String?,
    val durationSec: Long?,
    val thumbnail: String?,
    val formats: List<VideoFormatOption>
)

data class PlaylistEntryUi(
    val id: String,
    val url: String,
    val title: String,
    val durationSec: Long?
)

data class PlaylistUi(
    val url: String,
    val id: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val entries: List<PlaylistEntryUi>
)

sealed class MediaResult {
    data class Video(val video: VideoUi) : MediaResult()
    data class Playlist(val playlist: PlaylistUi) : MediaResult()
}
