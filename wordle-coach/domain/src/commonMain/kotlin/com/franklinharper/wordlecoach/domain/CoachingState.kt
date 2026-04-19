package com.franklinharper.wordlecoach.domain

data class CoachingState(
    val steps: List<CoachingStep>,
    val currentStepIndex: Int,
) {
    val currentStep: CoachingStep get() = steps[currentStepIndex]
    val canGoBack: Boolean get() = currentStepIndex > 0
    val canGoForward: Boolean get() = currentStepIndex < steps.size - 1

    fun goBack(): CoachingState =
        if (canGoBack) copy(currentStepIndex = currentStepIndex - 1) else this

    fun goForward(): CoachingState =
        if (canGoForward) copy(currentStepIndex = currentStepIndex + 1) else this

    companion object {
        /** Single-step state used before the user has shared a puzzle. */
        fun initial() = CoachingState(
            steps = listOf(CoachingStep.BeforeFirstGuess(guesses = emptyList())),
            currentStepIndex = 0,
        )

        /**
         * Builds all coaching steps from a completed puzzle.
         *
         * Step 0 is always [CoachingStep.BeforeFirstGuess] carrying all decoded guesses.
         * Steps 1..N correspond to [CoachingStep.AfterGuess] for each guess,
         * where remaining answers are filtered progressively.
         */
        fun fromPuzzle(puzzle: PuzzleResult): CoachingState {
            val steps = mutableListOf<CoachingStep>(
                CoachingStep.BeforeFirstGuess(guesses = puzzle.guesses)
            )
            var remaining = WordLists.answers
            for ((index, guess) in puzzle.guesses.withIndex()) {
                remaining = WordFilter.filter(remaining, guess)
                steps += CoachingStep.AfterGuess(
                    guessNumber = index + 1,
                    guess = guess,
                    remainingAnswers = remaining,
                )
            }
            return CoachingState(steps = steps, currentStepIndex = 0)
        }
    }
}
