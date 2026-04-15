package com.franklinharper.wordlecoach.androidApp

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.franklinharper.wordlecoach.domain.CompletedGuess
import com.franklinharper.wordlecoach.domain.GuessedTile
import com.franklinharper.wordlecoach.domain.LetterResult
import com.franklinharper.wordlecoach.domain.PuzzleResult
import org.json.JSONObject

private const val TAG = "WordleImageParser"

/**
 * Parses a Wordle screenshot into a [PuzzleResult] by sending the image to the Claude API
 * (vision) and asking Claude to read the tile letters and colours.
 *
 * Requires [apiKey] to be set; configure it via `anthropic.api.key` in `local.properties`.
 */
class WordleImageParser(private val context: Context, private val apiKey: String) {

    /**
     * Calls the Claude API with the image at [imageUri] and returns a parsed [PuzzleResult],
     * or `null` if the key is missing, the image can't be read, or Claude's response can't be
     * understood.
     *
     * This is a **blocking** call — always invoke it on a background thread
     * (e.g. `withContext(Dispatchers.IO)`).
     */
    fun parse(imageUri: Uri): PuzzleResult? {
        if (apiKey.isEmpty()) {
            Log.w(TAG, "ANTHROPIC_API_KEY not configured — add anthropic.api.key to local.properties")
            return null
        }

        val (imageBytes, mimeType) = readImage(imageUri) ?: return null
        val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val mediaType = if (mimeType?.contains("png", ignoreCase = true) == true)
            Base64ImageSource.MediaType.IMAGE_PNG
        else
            Base64ImageSource.MediaType.IMAGE_JPEG

        val imageBlock = ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(
                    Base64ImageSource.builder()
                        .mediaType(mediaType)
                        .data(base64Data)
                        .build()
                )
                .build()
        )

        val textBlock = ContentBlockParam.ofText(
            TextBlockParam.builder().text(PARSE_PROMPT).build()
        )

        val client: AnthropicClient = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()

        return try {
            val response = client.messages().create(
                MessageCreateParams.builder()
                    .model(Model.of("claude-opus-4-6"))
                    .addUserMessageOfBlockParams(listOf(imageBlock, textBlock))
                    .maxTokens(1024L)
                    .build()
            )
            val rawText = response.content()
                .firstNotNullOfOrNull { block -> block.text().orElse(null)?.text() }
                ?: run {
                    Log.w(TAG, "Claude returned no text content")
                    return null
                }
            parseJson(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Claude API call failed", e)
            null
        }
    }

    private fun readImage(uri: Uri): Pair<ByteArray, String?>? {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            bytes to mimeType
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read image URI: $uri", e)
            null
        }
    }

    private fun parseJson(raw: String): PuzzleResult? {
        return try {
            // Claude occasionally wraps JSON in markdown fences — strip them.
            val jsonStr = raw.trim().let { s ->
                val start = s.indexOf('{')
                val end = s.lastIndexOf('}')
                if (start >= 0 && end > start) s.substring(start, end + 1) else s
            }
            val guessesArr = JSONObject(jsonStr).getJSONArray("guesses")
            val guesses = (0 until guessesArr.length()).map { i ->
                val tilesArr = guessesArr.getJSONObject(i).getJSONArray("tiles")
                val tiles = (0 until tilesArr.length()).map { j ->
                    val tile = tilesArr.getJSONObject(j)
                    GuessedTile(
                        letter = tile.getString("letter").first().uppercaseChar(),
                        result = when (tile.getString("result")) {
                            "correct" -> LetterResult.Correct
                            "present" -> LetterResult.Present
                            else      -> LetterResult.Absent
                        }
                    )
                }
                CompletedGuess(tiles)
            }
            if (guesses.isEmpty()) null else PuzzleResult(guesses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response:\n$raw", e)
            null
        }
    }

    companion object {
        private val PARSE_PROMPT = """
            This is a screenshot of a completed Wordle puzzle.
            Examine the grid and return a JSON object describing each completed guess row.

            For each row that has colored tiles, read:
            - The letter shown on each tile (uppercase A-Z)
            - The tile color: "correct" (green), "present" (yellow/orange), or "absent" (gray/dark gray)

            Return ONLY this JSON — no markdown, no code fences, no explanation:
            {
              "guesses": [
                {
                  "tiles": [
                    {"letter": "R", "result": "absent"},
                    {"letter": "A", "result": "present"},
                    {"letter": "I", "result": "absent"},
                    {"letter": "S", "result": "absent"},
                    {"letter": "E", "result": "absent"}
                  ]
                }
              ]
            }

            Include only rows that have colored tiles (completed guesses). Skip any empty rows.
        """.trimIndent()
    }
}
