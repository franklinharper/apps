package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoachingStateTest {

    @Test fun initialStepIsZero() {
        assertEquals(0, CoachingState.initial().currentStepIndex)
    }

    @Test fun initialStepIsBeforeFirstGuess() {
        assertEquals(CoachingStep.BeforeFirstGuess, CoachingState.initial().currentStep)
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
            steps = listOf(CoachingStep.BeforeFirstGuess, CoachingStep.BeforeFirstGuess),
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
}
