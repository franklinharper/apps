package com.franklinharper.wordlecoach.domain

data class CompletedGuess(val tiles: List<GuessedTile>) {
    init {
        require(tiles.size == 5) { "A Wordle guess must have exactly 5 tiles, got ${tiles.size}" }
    }

    /** The guessed word as an uppercase 5-letter string. */
    val word: String get() = tiles.joinToString("") { it.letter.toString() }
}
