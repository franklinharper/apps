package com.franklinharper.wordlecoach.domain

enum class LetterResult {
    /** The letter is in the correct position (green tile). */
    Correct,
    /** The letter is in the answer but at a different position (yellow tile). */
    Present,
    /** The letter does not appear (or appears fewer times) in the answer (gray tile). */
    Absent,
}
