package com.franklinharper.wordlecoach.domain

object WordShape {
    private val VOWELS = setOf('A', 'E', 'I', 'O', 'U', 'Y')

    /**
     * Returns the C/V shape of a 5-letter word.
     * Y is treated as a vowel. Input is case-insensitive.
     * Example: "stung" -> "CCVCC"
     */
    fun of(word: String): String {
        require(word.length == 5) { "Word must be exactly 5 letters, got: \"$word\"" }
        return word.uppercase().map { if (it in VOWELS) 'V' else 'C' }.joinToString("")
    }
}
