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

    @Test
    fun `normalizer keeps full http url`() {
        assertEquals(
            "http://example.com/path",
            UrlNormalizer().normalize("http://example.com/path")
        )
    }

    @Test
    fun `normalizer preserves uppercase scheme`() {
        assertEquals(
            "HTTPS://example.com/path",
            UrlNormalizer().normalize("  HTTPS://example.com/path  ")
        )
    }
}
