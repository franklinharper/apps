package com.franklinharper.concentra.browser.url

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveTodayUrlBuilderTest {
    @Test
    fun `archive builder prefixes current url`() {
        assertEquals(
            "https://archive.ph/https://example.com",
            ArchiveTodayUrlBuilder().build("https://example.com")
        )
    }
}
