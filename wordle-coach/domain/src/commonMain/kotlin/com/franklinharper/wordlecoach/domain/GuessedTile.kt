package com.franklinharper.wordlecoach.domain

data class GuessedTile(val letter: Char, val result: LetterResult) {
    init {
        require(letter == letter.uppercaseChar()) { "Letter must be uppercase: $letter" }
    }
}
