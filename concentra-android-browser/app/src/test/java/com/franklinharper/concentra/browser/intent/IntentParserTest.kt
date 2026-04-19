package com.franklinharper.concentra.browser.intent

import android.content.Intent
import android.net.Uri
import com.franklinharper.concentra.browser.model.LaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntentParserTest {
    @Test
    fun `empty launch returns empty request`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_MAIN)

        assertEquals(LaunchRequest.Empty, parser.parse(intent))
    }

    @Test
    fun `view intent returns url request`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com"),
            parser.parse(intent)
        )
    }

    @Test
    fun `send intent extracts first url from text`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Read this https://example.com/page")
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com/page"),
            parser.parse(intent)
        )
    }
}
