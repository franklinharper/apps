package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordShapeStatsTest {

    @Test fun statsAreNotEmpty() {
        assertTrue(WordShapeStats.stats.isNotEmpty())
    }

    @Test fun percentagesSumToOneHundred() {
        val sum = WordShapeStats.stats.sumOf { it.percentage }
        assertEquals(100.0, sum, absoluteTolerance = 0.01)
    }

    @Test fun sortedByPercentageDescending() {
        val percentages = WordShapeStats.stats.map { it.percentage }
        assertEquals(percentages, percentages.sortedDescending())
    }

    @Test fun shapesAreFiveCharsOfCAndV() {
        for (stat in WordShapeStats.stats) {
            assertEquals(5, stat.shape.length, "Bad shape length: ${stat.shape}")
            assertTrue(stat.shape.all { it == 'C' || it == 'V' }, "Bad shape: ${stat.shape}")
        }
    }

    @Test fun countsMatchAnswerListSize() {
        val total = WordShapeStats.stats.sumOf { it.count }
        assertEquals(WordLists.answers.size, total)
    }

    @Test fun mostCommonShapeIsCVCVC() {
        // Empirically CVCVC (e.g. BRAVE, OLIVE, LOVER) is the most common shape in the NYT answer list
        assertEquals("CVCVC", WordShapeStats.stats.first().shape)
    }
}
