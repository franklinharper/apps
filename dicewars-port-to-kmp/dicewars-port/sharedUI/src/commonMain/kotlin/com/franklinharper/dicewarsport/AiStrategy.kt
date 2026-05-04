package com.franklinharper.dicewarsport

data class Move(val from: Int, val to: Int)

interface AiStrategy {
    fun chooseMove(game: DicewarsGame): Move?
}

internal fun List<Move>.randomOrNull(random: RandomSource): Move? =
    if (isEmpty()) null else this[random.nextInt(size)]
