package com.franklinharper.wordlecoach.domain

/**
 * Filters a list of candidate words by the constraints revealed in a completed guess.
 *
 * Duplicate-letter rules (mirrors Wordle's own logic):
 *
 * 1. Greens are matched first.
 * 2. Then yellows from the remaining unmatched letters in the answer.
 * 3. Any excess copies of a letter in the guess that can't be matched are marked Absent.
 *
 * This means when a letter appears as Absent in a guess that also contains the same
 * letter as Correct or Present, the count is *exact*:
 *   count(letter in answer) == count(Correct + Present tiles for that letter)
 *
 * When all copies of a letter in the guess are Correct/Present (no Absent copy),
 * the answer must contain *at least* that many of that letter.
 */
object WordFilter {

    fun filter(candidates: List<String>, guess: CompletedGuess): List<String> =
        candidates.filter { matches(it.uppercase(), guess) }

    private fun matches(word: String, guess: CompletedGuess): Boolean {
        val tiles = guess.tiles

        // ── 1. Correct-position check ──────────────────────────────────────
        for ((i, tile) in tiles.withIndex()) {
            if (tile.result == LetterResult.Correct && word[i] != tile.letter) return false
        }

        // ── 2. Present-position check (letter must NOT sit at its yellow index) ──
        for ((i, tile) in tiles.withIndex()) {
            if (tile.result == LetterResult.Present && word[i] == tile.letter) return false
        }

        // ── 3. Per-letter count constraints ───────────────────────────────
        //
        // Group tiles by letter and compute [minCount, maxCount].
        //   minCount = number of Correct + Present tiles for that letter
        //   maxCount = minCount  when any Absent tile exists for that letter
        //            = Int.MAX_VALUE  otherwise (at least minCount)
        //
        val letters = tiles.map { it.letter }.toSet()
        for (letter in letters) {
            val letterTiles = tiles.filter { it.letter == letter }
            val minCount = letterTiles.count {
                it.result == LetterResult.Correct || it.result == LetterResult.Present
            }
            val maxCount = if (letterTiles.any { it.result == LetterResult.Absent }) {
                minCount
            } else {
                Int.MAX_VALUE
            }
            val actualCount = word.count { it == letter }
            if (actualCount < minCount || actualCount > maxCount) return false
        }

        return true
    }
}
