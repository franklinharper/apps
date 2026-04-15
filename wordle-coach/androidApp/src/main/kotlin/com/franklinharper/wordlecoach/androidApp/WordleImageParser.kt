package com.franklinharper.wordlecoach.androidApp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.franklinharper.wordlecoach.domain.CompletedGuess
import com.franklinharper.wordlecoach.domain.GuessedTile
import com.franklinharper.wordlecoach.domain.LetterResult
import com.franklinharper.wordlecoach.domain.PuzzleResult
import kotlin.math.abs

private const val TAG = "WordleImageParser"

/**
 * Parses a Wordle screenshot into a [PuzzleResult] entirely on-device:
 *
 * 1. ML Kit Text Recognition (Latin, via Google Play Services) reads each letter and
 *    its bounding box.
 * 2. Pixel-colour analysis classifies each tile as Correct / Present / Absent using
 *    HSV thresholds tuned for the NYT Wordle light- and dark-mode palette.
 * 3. Tiles are grouped into rows; only 5-tile rows (grid guesses) are kept —
 *    coloured keyboard keys have 7–10 keys per row, so they are filtered out.
 *
 * No internet connection or API key required.
 *
 * **Call [parse] on a background thread** — ML Kit's [Tasks.await] blocks.
 */
class WordleImageParser(private val context: Context) {

    fun parse(imageUri: Uri): PuzzleResult? {
        val bitmap = loadBitmap(imageUri) ?: return null

        val visionText = try {
            Tasks.await(
                TextRecognition
                    .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(InputImage.fromBitmap(bitmap, 0))
            )
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit OCR failed", e)
            return null
        }

        // Collect single-letter elements that sit on a coloured (non-white) tile background.
        val coloredLetters = buildList {
            for (block in visionText.textBlocks)
                for (line in block.lines)
                    for (element in line.elements) {
                        val ch = element.text.trim()
                        if (ch.length != 1 || !ch[0].isLetter()) continue
                        val box = element.boundingBox ?: continue
                        val bgColor = sampleTileBackground(bitmap, box)
                        val result = classifyTileColor(bgColor) ?: continue
                        add(ColoredLetter(ch[0].uppercaseChar(), result, box))
                    }
        }

        if (coloredLetters.isEmpty()) return null

        // Group by vertical position, keep only complete 5-tile rows (grid rows).
        // Keyboard rows have 7–10 keys so they are naturally excluded.
        // Wordle allows at most 6 guesses, so cap at 6 rows top-to-bottom.
        val guesses = groupIntoRows(coloredLetters)
            .filter { it.size == 5 }
            .take(6)
            .map { row ->
                CompletedGuess(
                    row.sortedBy { it.box.left }
                        .map { GuessedTile(it.letter, it.result) }
                )
            }

        return if (guesses.isEmpty()) null else PuzzleResult(guesses)
    }

    // ── Bitmap loading ────────────────────────────────────────────────────────

    private fun loadBitmap(uri: Uri): Bitmap? =
        try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from $uri", e)
            null
        }

    // ── Colour sampling ───────────────────────────────────────────────────────

    /**
     * Samples the tile background colour by probing four points just outside the
     * ML Kit letter bounding box (above, below, left, right).
     *
     * Wordle tiles are noticeably larger than the letter glyph, so a margin equal
     * to ⅓ of the letter height reliably lands on the coloured tile background
     * rather than on the white letter strokes.
     *
     * Returns the most saturated of the four samples — i.e., the one most likely
     * to represent the tile colour rather than a neutral border or gap.
     */
    private fun sampleTileBackground(bitmap: Bitmap, box: Rect): Int {
        val margin = maxOf(box.width(), box.height()) / 3
        val cx = box.centerX()
        val cy = box.centerY()
        val candidates = listOf(
            bitmap.safePixel(cx, box.top    - margin),
            bitmap.safePixel(cx, box.bottom + margin),
            bitmap.safePixel(box.left  - margin, cy),
            bitmap.safePixel(box.right + margin, cy),
        )
        return candidates.maxByOrNull { pixel ->
            val hsv = FloatArray(3)
            Color.colorToHSV(pixel, hsv)
            hsv[1]   // highest saturation = most coloured → most likely tile background
        } ?: candidates.first()
    }

    /**
     * Maps a background pixel colour to a [LetterResult] using HSV ranges that
     * cover both NYT Wordle light-mode and dark-mode tile colours.
     *
     * Returns `null` for white / near-white pixels (empty tile or UI background).
     *
     * Colour reference:
     *  - Correct  green  #538D4E  H≈125° S≈45% V≈55%
     *  - Present  yellow #B59F3B  H≈ 51° S≈67% V≈71%
     *  - Absent   grey   #787C7E  H≈200° S≈ 2% V≈49%  (light mode)
     *             dark   #3A3A3C  H≈240° S≈ 3% V≈24%  (dark mode)
     */
    private fun classifyTileColor(pixel: Int): LetterResult? {
        val hsv = FloatArray(3)
        Color.colorToHSV(pixel, hsv)
        val (h, s, v) = Triple(hsv[0], hsv[1], hsv[2])

        if (s < 0.12f && v > 0.80f) return null              // white / near-white → empty tile
        if (h in 85f..170f && s > 0.20f) return LetterResult.Correct   // green
        if (h in 30f..85f  && s > 0.25f) return LetterResult.Present   // yellow / gold
        return LetterResult.Absent                             // grey or dark → absent
    }

    // ── Row grouping ──────────────────────────────────────────────────────────

    /**
     * Clusters [ColoredLetter]s into rows by their vertical-centre coordinate.
     * Two letters are in the same row when their centres are within 70% of the
     * first tile's height of each other — far enough to handle ML Kit's box
     * variation, tight enough to separate adjacent Wordle rows.
     */
    private fun groupIntoRows(letters: List<ColoredLetter>): List<List<ColoredLetter>> {
        val sorted = letters.sortedBy { it.box.centerY() }
        // Use median letter height as the row-separation tolerance so a single
        // unusually-sized bounding box doesn't skew the threshold.
        val medianHeight = sorted.map { it.box.height() }.sorted()[sorted.size / 2]
        val tolerance = medianHeight * 0.7
        val rows = mutableListOf<MutableList<ColoredLetter>>()
        var current = mutableListOf(sorted.first())

        for (letter in sorted.drop(1)) {
            if (abs(letter.box.centerY() - current.last().box.centerY()) <= tolerance) {
                current.add(letter)
            } else {
                rows.add(current)
                current = mutableListOf(letter)
            }
        }
        rows.add(current)
        return rows
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Bitmap.safePixel(x: Int, y: Int): Int =
        getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))

    private data class ColoredLetter(val letter: Char, val result: LetterResult, val box: Rect)
}
