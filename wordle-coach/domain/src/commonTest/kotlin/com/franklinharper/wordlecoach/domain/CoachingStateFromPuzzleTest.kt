package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun guess(vararg tiles: Pair<Char, LetterResult>): CompletedGuess =
    CompletedGuess(tiles.map { (l, r) -> GuessedTile(l.uppercaseChar(), r) })

class CoachingStateFromPuzzleTest {

    private val craneAllAbsent = guess(
        'C' to LetterResult.Absent, 'R' to LetterResult.Absent,
        'A' to LetterResult.Absent, 'N' to LetterResult.Absent, 'E' to LetterResult.Absent
    )

    @Test fun stepsCountIsGuessesPlus1() {
        // 1 guess → 2 steps: BeforeFirstGuess + AfterGuess(1)
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent)))
        assertEquals(2, state.steps.size)
    }

    @Test fun firstStepIsAlwaysBeforeFirstGuess() {
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent)))
        assertIs<CoachingStep.BeforeFirstGuess>(state.steps[0])
    }

    @Test fun secondStepIsAfterGuess1() {
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent)))
        val step = assertIs<CoachingStep.AfterGuess>(state.steps[1])
        assertEquals(1, step.guessNumber)
        assertEquals(craneAllAbsent, step.guess)
    }

    @Test fun afterGuessRemainingAnswersAreFewer() {
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent)))
        val step = assertIs<CoachingStep.AfterGuess>(state.steps[1])
        assertTrue(step.remainingAnswers.size < WordLists.answers.size)
    }

    @Test fun multipleGuessesProduceProgressivelyFewerRemaining() {
        val guess2 = guess(
            'S' to LetterResult.Present, 'L' to LetterResult.Absent,
            'U' to LetterResult.Absent,  'M' to LetterResult.Absent, 'P' to LetterResult.Absent
        )
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent, guess2)))
        assertEquals(3, state.steps.size)
        val step1 = assertIs<CoachingStep.AfterGuess>(state.steps[1])
        val step2 = assertIs<CoachingStep.AfterGuess>(state.steps[2])
        assertEquals(1, step1.guessNumber)
        assertEquals(2, step2.guessNumber)
        assertTrue(step2.remainingAnswers.size < step1.remainingAnswers.size)
    }

    @Test fun startsAtStepZero() {
        val state = CoachingState.fromPuzzle(PuzzleResult(listOf(craneAllAbsent)))
        assertEquals(0, state.currentStepIndex)
    }
}
