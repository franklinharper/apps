package com.franklinharper.concentra.browser.intent

import android.content.Intent
import android.net.Uri
import com.franklinharper.concentra.browser.model.LaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntentParserTest {
    @Test
    fun `empty launch returns empty request`() {
        val parser = IntentParser()
        val intent = Intent()

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
    fun `view intent ignores unsupported scheme`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("javascript:alert('xss')"))

        assertEquals(LaunchRequest.Empty, parser.parse(intent))
    }

    @Test
    fun `send intent extracts first url from text`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "See https://example.com/page). and also https://example.com/second,"
            )
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com/page"),
            parser.parse(intent)
        )
    }

    @Test
    fun `send intent returns empty when shared text has no url`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "This message has no link in it")
        }

        assertEquals(LaunchRequest.Empty, parser.parse(intent))
    }

    @Test
    fun `send intent preserves balanced closing parenthesis in url`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Read https://example.com/wiki/Function_(mathematics)")
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com/wiki/Function_(mathematics)"),
            parser.parse(intent)
        )
    }
}
