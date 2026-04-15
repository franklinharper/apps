package com.franklinharper.wordlecoach.domain

data class PuzzleResult(val guesses: List<CompletedGuess>) {
    init {
        require(guesses.isNotEmpty()) { "A puzzle must have at least one guess" }
        require(guesses.size <= 6) { "A Wordle puzzle has at most 6 guesses, got ${guesses.size}" }
    }
}
