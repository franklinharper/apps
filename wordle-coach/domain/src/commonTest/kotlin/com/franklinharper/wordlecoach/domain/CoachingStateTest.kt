package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoachingStateTest {

    private val crane = CompletedGuess(
        listOf(
            GuessedTile('C', LetterResult.Absent),
            GuessedTile('R', LetterResult.Present),
            GuessedTile('A', LetterResult.Absent),
            GuessedTile('N', LetterResult.Absent),
            GuessedTile('E', LetterResult.Correct),
        )
    )
    private val snout = CompletedGuess(
        listOf(
            GuessedTile('S', LetterResult.Correct),
            GuessedTile('N', LetterResult.Correct),
            GuessedTile('O', LetterResult.Correct),
            GuessedTile('U', LetterResult.Correct),
            GuessedTile('T', LetterResult.Correct),
        )
    )

    @Test
    fun initial_step_has_empty_guesses() {
        val state = CoachingState.initial()
        val step = assertIs<CoachingStep.BeforeFirstGuess>(state.currentStep)
        assertEquals(emptyList(), step.guesses)
    }

    @Test
    fun fromPuzzle_step_zero_carries_all_guesses() {
        val puzzle = PuzzleResult(listOf(crane, snout))
        val state = CoachingState.fromPuzzle(puzzle)
        val step = assertIs<CoachingStep.BeforeFirstGuess>(state.currentStep)
        assertEquals(listOf(crane, snout), step.guesses)
    }
}
