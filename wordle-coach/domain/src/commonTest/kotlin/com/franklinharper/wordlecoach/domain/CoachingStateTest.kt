package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoachingStateTest {

    @Test fun initialStepIsZero() {
        assertEquals(0, CoachingState.initial().currentStepIndex)
    }

    @Test fun initialStepIsBeforeFirstGuess() {
        assertIs<CoachingStep.BeforeFirstGuess>(CoachingState.initial().currentStep)
    }

    @Test fun cannotGoBackFromFirstStep() {
        assertFalse(CoachingState.initial().canGoBack)
    }

    @Test fun cannotGoForwardWhenOnlyOneStep() {
        assertFalse(CoachingState.initial().canGoForward)
    }

    @Test fun goBackDoesNothingAtFirstStep() {
        val state = CoachingState.initial()
        assertEquals(state, state.goBack())
    }

    @Test fun goForwardDoesNothingAtLastStep() {
        val state = CoachingState.initial()
        assertEquals(state, state.goForward())
    }

    @Test fun canNavigateForwardAndBack() {
        val twoSteps = CoachingState(
            steps = listOf(
                CoachingStep.BeforeFirstGuess(guesses = emptyList()),
                CoachingStep.BeforeFirstGuess(guesses = emptyList()),
            ),
            currentStepIndex = 0
        )
        assertFalse(twoSteps.canGoBack)
        assertTrue(twoSteps.canGoForward)

        val atStep1 = twoSteps.goForward()
        assertEquals(1, atStep1.currentStepIndex)
        assertTrue(atStep1.canGoBack)
        assertFalse(atStep1.canGoForward)

        val backAtStep0 = atStep1.goBack()
        assertEquals(0, backAtStep0.currentStepIndex)
    }

    // --- New tests for BeforeFirstGuess carrying decoded guesses (Task 1) ---

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
