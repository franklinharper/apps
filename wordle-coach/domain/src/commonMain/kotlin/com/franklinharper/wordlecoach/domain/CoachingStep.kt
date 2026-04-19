package com.franklinharper.wordlecoach.domain

sealed class CoachingStep {

    /** The state before the player makes their first guess. */
    data class BeforeFirstGuess(val guesses: List<CompletedGuess>) : CoachingStep()

    /**
     * The state immediately after the player has submitted guess [guessNumber] (1-based).
     *
     * @param guess             The tiles and results for that guess.
     * @param remainingAnswers  Answer-list words still consistent with all guesses so far.
     */
    data class AfterGuess(
        val guessNumber: Int,
        val guess: CompletedGuess,
        val remainingAnswers: List<String>,
    ) : CoachingStep()
}
