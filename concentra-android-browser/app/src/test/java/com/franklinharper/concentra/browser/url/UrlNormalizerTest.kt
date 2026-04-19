package com.franklinharper.concentra.browser.url

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun `normalizer adds https scheme to bare host`() {
        assertEquals(
            "https://example.com",
            UrlNormalizer().normalize("example.com")
        )
    }

    @Test
    fun `normalizer keeps full https url`() {
        assertEquals(
            "https://example.com/path",
            UrlNormalizer().normalize("https://example.com/path")
        )
    }
}
