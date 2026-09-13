package com.raebae.ytdl

import com.raebae.ytdl.data.FormatBuilder
import com.raebae.ytdl.data.FormatBuilder.RawFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatBuilderTest {

    private fun raw(
        id: String,
        ext: String? = "mp4",
        vcodec: String? = "avc1.640028",
        acodec: String? = "none",
        height: Int? = 1080,
        fps: Float? = 30f,
        tbr: Float? = 4000f,
        abr: Float? = null,
        filesize: Long? = null,
        filesizeApprox: Long? = null,
        protocol: String? = "https"
    ) = RawFormat(id, ext, vcodec, acodec, height, fps, tbr, abr, filesize, filesizeApprox, protocol)

    @Test
    fun `progressive format yields single-id spec`() {
        val options = FormatBuilder.buildOptions(
            listOf(raw("18", vcodec = "avc1.42001E", acodec = "mp4a.40.2", height = 360, tbr = 500f)),
            durationSec = 100
        )
        assertEquals(1, options.size)
        assertEquals("18", options[0].formatSpec)
        assertNull(options[0].audioSpec)
        assertTrue(options[0].isProgressive)
    }

    @Test
    fun `video-only pairs with mp4a for mp4 container`() {
        val formats = listOf(
            raw("137", vcodec = "avc1.640028", acodec = "none", height = 1080),
            raw("140", vcodec = "none", acodec = "mp4a.40.2", height = null, tbr = null, abr = 128f),
            raw("251", ext = "webm", vcodec = "none", acodec = "opus", height = null, tbr = null, abr = 130f)
        )
        val options = FormatBuilder.buildOptions(formats, 100)
        assertEquals(1, options.size)
        assertEquals("137+140", options[0].formatSpec)
        assertEquals("AAC", options[0].audioCodecLabel)
    }

    @Test
    fun `webm video pairs with opus audio`() {
        val formats = listOf(
            raw("248", ext = "webm", vcodec = "vp09.00.50.08", acodec = "none", height = 1080),
            raw("140", vcodec = "none", acodec = "mp4a.40.2", height = null, abr = 128f),
            raw("251", ext = "webm", vcodec = "none", acodec = "opus", height = null, abr = 130f)
        )
        val options = FormatBuilder.buildOptions(formats, 100)
        assertEquals(1, options.size)
        assertEquals("248+251", options[0].formatSpec)
        assertEquals("Opus", options[0].audioCodecLabel)
        assertEquals("VP9", options[0].vcodecLabel)
    }

    @Test
    fun `formats sorted by height descending and deduped`() {
        val formats = listOf(
            raw("a", height = 720, tbr = 2000f),
            raw("b", height = 1080, tbr = 4000f),
            raw("c", height = 1080, tbr = 4500f)
        )
        val options = FormatBuilder.buildOptions(formats, 100)
        assertEquals(2, options.size)
        assertEquals(1080, options[0].height)
        assertEquals(720, options[1].height)
    }

    @Test
    fun `m3u8 and heightless formats skipped`() {
        val formats = listOf(
            raw("hls", protocol = "m3u8_native", height = 1080),
            raw("audio", vcodec = "none", acodec = "mp4a.40.2", height = null, abr = 128f),
            raw("sb", vcodec = "none", acodec = "none", height = null)
        )
        val options = FormatBuilder.buildOptions(formats, 100)
        assertTrue(options.isEmpty())
    }

    @Test
    fun `size estimated from bitrate and duration when missing`() {
        val options = FormatBuilder.buildOptions(
            listOf(raw("137", tbr = 4000f, filesize = null)),
            durationSec = 100
        )
        // 4000 kbit/s * 100 s / 8 = 50000 KB = 51_200_000 bytes approx
        assertTrue((options[0].sizeBytes ?: 0) > 40_000_000)
    }

    @Test
    fun `playlist template contains height and codec filters`() {
        val t = FormatBuilder.playlistTemplate(720, "avc1")
        assertTrue(t.contains("[height<=720]"))
        assertTrue(t.contains("[vcodec^=avc1]"))
        assertTrue(t.contains("+ba"))
        assertTrue(t.endsWith("/b"))
    }

    @Test
    fun `playlist template without codec`() {
        val t = FormatBuilder.playlistTemplate(1080, null)
        assertTrue(!t.contains("vcodec"))
        assertTrue(t.contains("[height<=1080]"))
    }

    @Test
    fun `merge ext follows codec`() {
        assertEquals("webm", FormatBuilder.mergeExtFor("vp9", "webm"))
        assertEquals("webm", FormatBuilder.mergeExtFor("vp09", null))
        assertEquals("webm", FormatBuilder.mergeExtFor("vp09", "mp4"))
        assertEquals("mp4", FormatBuilder.mergeExtFor("avc1", "mp4"))
        assertEquals("mp4", FormatBuilder.mergeExtFor("av01", "mp4"))
        assertEquals("mp4", FormatBuilder.mergeExtFor(null, "mp4"))
    }

    @Test
    fun `file name sanitized`() {
        assertEquals("a b c d e f g h i j", FormatBuilder.sanitizeFileName("a/b\\c:d*e?f\"g<h>i|j"))
        assertEquals("видео", FormatBuilder.sanitizeFileName("  видео  "))
        assertEquals("video", FormatBuilder.sanitizeFileName("???"))
    }

    @Test
    fun `video-only formats kept as silent when no audio exists`() {
        val options = FormatBuilder.buildOptions(
            listOf(raw("b", height = 1080, tbr = 4000f)),
            durationSec = 100
        )
        assertEquals(1, options.size)
        assertEquals("b", options[0].formatSpec)
        assertNull(options[0].audioSpec)
    }

    @Test
    fun `codec labels`() {
        assertEquals("H.264", FormatBuilder.codecLabel("avc1.640028"))
        assertEquals("VP9", FormatBuilder.codecLabel("vp09.00.50.08"))
        assertEquals("AV1", FormatBuilder.codecLabel("av01.0.08M.08"))
        assertEquals("HEVC", FormatBuilder.codecLabel("hvc1.1.6.L123.00"))
        assertNull(FormatBuilder.codecLabel("none"))
        assertEquals("AAC", FormatBuilder.audioLabel("mp4a.40.2"))
        assertEquals("Opus", FormatBuilder.audioLabel("opus"))
    }
}
