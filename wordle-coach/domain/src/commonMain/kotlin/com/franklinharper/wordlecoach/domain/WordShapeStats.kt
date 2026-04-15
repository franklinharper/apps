package com.franklinharper.wordlecoach.domain

data class ShapeStat(val shape: String, val count: Int, val percentage: Double)

object WordShapeStats {
    /**
     * Shape statistics derived from the answer list, sorted by percentage descending.
     */
    val stats: List<ShapeStat> by lazy {
        val total = WordLists.answers.size.toDouble()
        WordLists.answers
            .groupingBy { WordShape.of(it) }
            .eachCount()
            .map { (shape, count) -> ShapeStat(shape, count, count / total * 100.0) }
            .sortedByDescending { it.percentage }
    }
}
