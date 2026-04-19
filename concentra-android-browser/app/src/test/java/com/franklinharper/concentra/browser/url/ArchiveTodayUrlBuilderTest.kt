package com.franklinharper.concentra.browser.url

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveTodayUrlBuilderTest {
    @Test
    fun `archive builder prefixes current url`() {
        assertEquals(
            "https://archive.ph/https%3A%2F%2Fexample.com",
            ArchiveTodayUrlBuilder().build("https://example.com")
        )
    }

    @Test
    fun `archive builder encodes query and fragment characters`() {
        assertEquals(
            "https://archive.ph/https%3A%2F%2Fexample.com%3Fa%3D1%23frag",
            ArchiveTodayUrlBuilder().build("https://example.com?a=1#frag")
        )
    }
}
