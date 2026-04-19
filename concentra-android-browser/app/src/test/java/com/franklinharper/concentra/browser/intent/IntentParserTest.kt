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
    fun `null intent returns empty request`() {
        val parser = IntentParser()

        assertEquals(LaunchRequest.Empty, parser.parse(null))
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
    fun `view intent accepts local html content uri`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://docs/example.html"), "text/html")
        }

        assertEquals(
            LaunchRequest.OpenUrl("content://docs/example.html"),
            parser.parse(intent),
        )
    }

    @Test
    fun `view intent accepts local html file uri`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("file:///sdcard/Download/example.html"), "text/html")
        }

        assertEquals(
            LaunchRequest.OpenUrl("file:///sdcard/Download/example.html"),
            parser.parse(intent),
        )
    }

    @Test
    fun `view intent rejects non html local content uri`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://docs/example.txt"), "text/plain")
        }

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
    fun `send intent accepts char sequence extra text`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                StringBuilder("Open https://example.com/from-builder") as CharSequence
            )
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com/from-builder"),
            parser.parse(intent)
        )
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

    @Test
    fun `send intent preserves valid trailing query delimiter`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Open https://example.com/?")
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com/?"),
            parser.parse(intent)
        )
    }

    @Test
    fun `send intent accepts uppercase url scheme in shared text`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Open HTTPS://example.com/Uppercase")
        }

        assertEquals(
            LaunchRequest.OpenUrl("HTTPS://example.com/Uppercase"),
            parser.parse(intent)
        )
    }

    @Test
    fun `send intent excludes trailing exclamation semicolon and colon punctuation`() {
        val parser = IntentParser()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check these https://example.com! https://example.org; https://example.net:"
            )
        }

        assertEquals(
            LaunchRequest.OpenUrl("https://example.com"),
            parser.parse(intent)
        )
    }
}
