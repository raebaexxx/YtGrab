package com.raebae.ytdl.data

/**
 * Pure format-selection logic (no Android/JSON dependencies so it is unit-testable).
 */
object FormatBuilder {

    data class RawFormat(
        val formatId: String,
        val ext: String?,
        val vcodec: String?,
        val acodec: String?,
        val height: Int?,
        val fps: Float?,
        val tbr: Float?,
        val abr: Float?,
        val filesize: Long?,
        val filesizeApprox: Long?,
        val protocol: String?
    )

    private val codecPriority = mapOf("H.264" to 0, "VP9" to 1, "AV1" to 2)

    fun codecLabel(vcodec: String?): String? = when {
        vcodec == null || vcodec == "none" -> null
        vcodec.startsWith("avc1") || vcodec.startsWith("avc3") -> "H.264"
        vcodec.startsWith("vp09") || vcodec.startsWith("vp9") -> "VP9"
        vcodec.startsWith("av01") -> "AV1"
        vcodec.startsWith("hvc1") || vcodec.startsWith("hev1") -> "HEVC"
        else -> vcodec.substringBefore('.').uppercase()
    }

    fun audioLabel(acodec: String?): String? = when {
        acodec == null || acodec == "none" -> null
        acodec.startsWith("mp4a") -> "AAC"
        acodec.startsWith("opus") -> "Opus"
        acodec.startsWith("vorbis") -> "Vorbis"
        acodec.startsWith("ec-3") || acodec.startsWith("ac-3") -> "Dolby"
        else -> acodec.substringBefore('.').uppercase()
    }

    fun isPlayableProtocol(f: RawFormat): Boolean =
        f.protocol == null || f.protocol.startsWith("http")

    fun isAudioOnly(f: RawFormat): Boolean =
        f.acodec != null && f.acodec != "none" && (f.vcodec == null || f.vcodec == "none")

    fun isVideo(f: RawFormat): Boolean =
        f.vcodec != null && f.vcodec != "none"

    fun sizeOf(f: RawFormat, durationSec: Long?): Long? {
        f.filesize?.let { return it }
        f.filesizeApprox?.let { return it }
        val bitrate = f.tbr ?: f.abr ?: return null
        val d = durationSec ?: return null
        return if (d > 0) (bitrate * 1000.0 / 8.0 * d).toLong() else null
    }

    private fun preferredAudio(ext: String?, f: RawFormat): Boolean = when (ext?.lowercase()) {
        "webm" -> (f.acodec ?: "").startsWith("opus")
        else -> (f.acodec ?: "").startsWith("mp4a")
    }

    fun bestAudio(audioOnly: List<RawFormat>, videoExt: String?): RawFormat? {
        if (audioOnly.isEmpty()) return null
        val preferred = audioOnly.filter { preferredAudio(videoExt, it) }
        val pool = preferred.ifEmpty { audioOnly }
        return pool.maxByOrNull { it.abr ?: it.tbr ?: 0f }
    }

    fun buildOptions(raw: List<RawFormat>, durationSec: Long?): List<VideoFormatOption> {
        val audioOnly = raw.filter { isAudioOnly(it) }
        val options = ArrayList<VideoFormatOption>()

        for (f in raw) {
            if (!isVideo(f)) continue
            val height = f.height ?: continue
            if (height <= 0) continue
            if (!isPlayableProtocol(f)) continue

            val progressive = f.acodec != null && f.acodec != "none"
            val audio: RawFormat? = if (progressive) {
                null
            } else if (audioOnly.isEmpty()) {
                null
            } else {
                bestAudio(audioOnly, f.ext) ?: continue
            }

            val vSize = sizeOf(f, durationSec)
            val aSize = audio?.let { sizeOf(it, durationSec) }
            val total = when {
                vSize == null && aSize == null -> null
                else -> (vSize ?: 0L) + (aSize ?: 0L)
            }

            val spec = when {
                progressive -> f.formatId
                audio != null -> "${f.formatId}+${audio.formatId}"
                else -> f.formatId
            }

            options += VideoFormatOption(
                formatSpec = spec,
                height = height,
                fps = (f.fps ?: 0f).toInt(),
                ext = f.ext ?: "mp4",
                vcodec = f.vcodec ?: "",
                vcodecLabel = codecLabel(f.vcodec) ?: f.vcodec ?: "?",
                audioSpec = audio?.formatId,
                audioCodecLabel = audioLabel(audio?.acodec),
                sizeBytes = total,
                isProgressive = progressive
            )
        }

        return options
            .groupBy { Triple(it.height, it.fps, it.vcodecLabel + "|" + it.ext) }
            .map { (_, group) ->
                group.maxByOrNull { it.sizeBytes ?: Long.MAX_VALUE } ?: group.first()
            }
            .sortedWith(
                compareByDescending<VideoFormatOption> { it.height }
                    .thenByDescending { it.fps }
                    .thenBy { codecPriority[it.vcodecLabel] ?: 3 }
            )
    }

    /** yt-dlp format template for playlist items, e.g. height=1080, vcodecPrefix="avc1". */
    fun playlistTemplate(maxHeight: Int, vcodecPrefix: String?): String {
        val base = "bv*[height<=$maxHeight]"
        val withCodec = if (vcodecPrefix != null) "$base[vcodec^=$vcodecPrefix]" else base
        val h = maxHeight
        return "$withCodec+ba/bv*[height<=$h]+ba/b[height<=$h]/b"
    }

    fun mergeExtFor(vcodecPrefix: String?, videoExt: String?): String = when {
        vcodecPrefix?.startsWith("vp9") == true -> "webm"
        videoExt?.lowercase() == "webm" -> "webm"
        else -> "mp4"
    }

    fun sanitizeFileName(title: String): String = title
        .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1f]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(110)
        .trim()
        .ifEmpty { "video" }
}
