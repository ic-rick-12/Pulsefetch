package com.example

import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun extractFirstUrl_fromSharedText_returnsCleanUrl() {
        val shared = "Check out this clip! https://archive.org/details/BigBuckBunny_124."
        assertEquals(
            "https://archive.org/details/BigBuckBunny_124",
            FormatUtils.extractFirstUrl(shared)
        )
        assertNull(FormatUtils.extractFirstUrl("Just some plain text without a link"))
    }

    @Test
    fun formatBytes_and_formatSpeed_and_formatEta_formatCorrectly() {
        assertEquals("02:05", FormatUtils.formatDuration(125L))
        assertTrue(FormatUtils.formatBytes(10L * 1024L * 1024L).contains("MB"))
        assertTrue(FormatUtils.formatSpeed(2L * 1024L * 1024L).contains("/s"))
        assertEquals("1m 5s", FormatUtils.formatEta(65L))
    }

    @Test
    fun extractDisplayDomain_and_isLikelyUrl_workCorrectly() {
        assertEquals("archive.org", FormatUtils.extractDisplayDomain("https://www.archive.org/details/BigBuckBunny"))
        assertTrue(FormatUtils.isLikelyUrl("https://example.com/video.mp4"))
    }
}
