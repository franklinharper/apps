package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Helper — builds a CompletedGuess from pairs of (letter, result).
 * Letters are uppercased automatically.
 */
private fun guess(vararg tiles: Pair<Char, LetterResult>): CompletedGuess =
    CompletedGuess(tiles.map { (l, r) -> GuessedTile(l.uppercaseChar(), r) })

class WordFilterTest {

    // ── Absent ───────────────────────────────────────────────────────────────

    @Test fun absentLetterExcludesWord() {
        // S is absent → "SISSY" must be filtered out
        val g = guess('S' to LetterResult.Absent, 'L' to LetterResult.Absent,
                      'A' to LetterResult.Absent, 'T' to LetterResult.Absent, 'E' to LetterResult.Absent)
        val results = WordFilter.filter(listOf("SISSY", "ROBIN"), g)
        assertFalse("SISSY" in results)
        assertTrue("ROBIN" in results)
    }

    // ── Correct ──────────────────────────────────────────────────────────────

    @Test fun correctLetterMustBeAtThatPosition() {
        // C correct at pos 0 → only words starting with C pass
        val g = guess('C' to LetterResult.Correct, 'R' to LetterResult.Absent,
                      'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent)
        val results = WordFilter.filter(listOf("CRANE", "COULD", "BRAVE"), g)
        assertFalse("CRANE" in results, "CRANE has A, N, E — all absent")
        assertTrue("COULD" in results)
        assertFalse("BRAVE" in results, "doesn't start with C")
    }

    @Test fun wrongLetterAtCorrectPositionExcludes() {
        val g = guess('C' to LetterResult.Correct, 'R' to LetterResult.Absent,
                      'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent)
        assertFalse(WordFilter.filter(listOf("BRAVE"), g).contains("BRAVE"))
    }

    // ── Present ──────────────────────────────────────────────────────────────

    @Test fun presentLetterMustAppearInWord() {
        // R present → candidate must contain R somewhere
        val g = guess('C' to LetterResult.Absent, 'R' to LetterResult.Present,
                      'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent)
        assertFalse(WordFilter.filter(listOf("COULD"), g).contains("COULD"))
    }

    @Test fun presentLetterMustNotBeAtSamePosition() {
        // R present at pos 1 → candidate with R at pos 1 is excluded
        val g = guess('C' to LetterResult.Absent, 'R' to LetterResult.Present,
                      'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent)
        // TRIED has R at pos 1 — same as the Present tile → excluded
        assertFalse(WordFilter.filter(listOf("TRIED"), g).contains("TRIED"))
        // BIRTH = B,I,R,T,H — R at pos 2 (not pos 1), no C/A/N/E → passes all constraints
        assertTrue(WordFilter.filter(listOf("BIRTH"), g).contains("BIRTH"))
    }

    // ── Duplicate letters ────────────────────────────────────────────────────

    /**
     * SPEED vs ABBEY
     *
     * ABBEY = A B B E Y  (positions 0-4)
     * SPEED = S P E E D
     *
     * Wordle resolves greens first:
     *   pos 3: E matches E → Correct
     * Then yellows from remaining unmatched letters (ABBEY has no more E):
     *   pos 2: E → no unmatched E left → Absent
     *
     * Tiles: S=Absent, P=Absent, E(2)=Absent, E(3)=Correct, D=Absent
     * Constraint: exactly 1 E (minCount=1 from Correct, Absent present → maxCount=1).
     */
    @Test fun duplicateGuessLetterAbsentMeansExactCount() {
        val g = guess(
            'S' to LetterResult.Absent,
            'P' to LetterResult.Absent,
            'E' to LetterResult.Absent,   // pos 2 — no free E left in answer
            'E' to LetterResult.Correct,  // pos 3
            'D' to LetterResult.Absent
        )
        // GEESE has 3 E's — exceeds max of 1 → excluded
        assertFalse(WordFilter.filter(listOf("GEESE"), g).contains("GEESE"))
        // GREED has 2 E's — exceeds max of 1 → excluded
        assertFalse(WordFilter.filter(listOf("GREED"), g).contains("GREED"))
        // ABBEY has exactly 1 E and E is at position 3 → passes
        assertTrue(WordFilter.filter(listOf("ABBEY"), g).contains("ABBEY"))
    }

    /**
     * SPEED vs GREED (both E's are Correct → min 2 E's, no Absent E → max ∞).
     */
    @Test fun twoCorrectSameLetterRequiresAtLeastTwo() {
        val g = guess(
            'S' to LetterResult.Absent,
            'P' to LetterResult.Absent,
            'E' to LetterResult.Correct,  // pos 2
            'E' to LetterResult.Correct,  // pos 3
            'D' to LetterResult.Correct
        )
        // ABBEY only has 1 E → excluded
        assertFalse(WordFilter.filter(listOf("ABBEY"), g).contains("ABBEY"))
        // GREED has 2 E's and E at pos 2 & 3, D at pos 4 → passes
        assertTrue(WordFilter.filter(listOf("GREED"), g).contains("GREED"))
    }

    // ── All-green ────────────────────────────────────────────────────────────

    @Test fun allCorrectMatchesOnlyThatWord() {
        val g = guess('C' to LetterResult.Correct, 'R' to LetterResult.Correct,
                      'A' to LetterResult.Correct, 'N' to LetterResult.Correct, 'E' to LetterResult.Correct)
        val results = WordFilter.filter(listOf("CRANE", "BRAVE", "STUNG"), g)
        assertEquals(listOf("CRANE"), results)
    }

    // ── Progressive filtering ─────────────────────────────────────────────────

    @Test fun filteringReducesCandidatesMonotonically() {
        val afterGuess1 = WordFilter.filter(WordLists.answers, guess(
            'C' to LetterResult.Absent, 'R' to LetterResult.Absent,
            'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent
        ))
        val afterGuess2 = WordFilter.filter(afterGuess1, guess(
            'S' to LetterResult.Present, 'L' to LetterResult.Absent,
            'U' to LetterResult.Absent, 'M' to LetterResult.Absent, 'P' to LetterResult.Absent
        ))
        assertTrue(afterGuess1.size < WordLists.answers.size)
        assertTrue(afterGuess2.size < afterGuess1.size)
    }
}
