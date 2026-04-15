package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WordShapeTest {

    @Test fun consonantVowelPattern() {
        // S=C, T=C, U=V, N=C, G=C
        assertEquals("CCVCC", WordShape.of("stung"))
    }

    @Test fun allVowels() {
        // A=V, U=V, D=C, I=V, O=V
        assertEquals("VVCVV", WordShape.of("audio"))
    }

    @Test fun yIsVowel() {
        // Y=V, A=V, C=C, H=C, T=C
        assertEquals("VVCCC", WordShape.of("yacht"))
    }

    @Test fun yAsMiddleVowel() {
        // S=C, T=C, Y=V, L=C, E=V
        assertEquals("CCVCV", WordShape.of("style"))
    }

    @Test fun uppercaseInput() {
        assertEquals("CCVCC", WordShape.of("STUNG"))
    }

    @Test fun mixedCaseInput() {
        assertEquals("CCVCC", WordShape.of("StUnG"))
    }

    @Test fun wrongLengthThrows() {
        assertFailsWith<IllegalArgumentException> { WordShape.of("hi") }
        assertFailsWith<IllegalArgumentException> { WordShape.of("toolong") }
    }

    @Test fun resultIsAlwaysFiveChars() {
        assertEquals(5, WordShape.of("crane").length)
    }

    @Test fun resultContainsOnlyCAndV() {
        val shape = WordShape.of("crane")
        assert(shape.all { it == 'C' || it == 'V' }) { "Unexpected chars in: $shape" }
    }
}
