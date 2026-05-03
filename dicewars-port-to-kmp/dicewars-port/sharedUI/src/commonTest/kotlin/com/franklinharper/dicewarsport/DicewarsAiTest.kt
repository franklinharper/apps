package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DicewarsAiTest {

    @Test
    fun aiStrategiesNeverReturnIllegalMoves() {
        val strategies = listOf(ExampleAi(FixedAiRandom(0)), DefaultAi(FixedAiRandom(0)), DefensiveAi())
        for (strategy in strategies) {
            val game = aiGame()
            val move = strategy.chooseMove(game)
            assertNotNull(move, strategy::class.simpleName ?: "strategy")
            assertTrue(game.isLegalAttack(move.from, move.to), "${strategy::class.simpleName} returned illegal $move")
        }
    }

    @Test
    fun exampleAiAttacksOnlyWeakerAdjacentEnemyAreas() {
        val game = aiGame()
        game.areas[2].dice = game.areas[1].dice

        val move = ExampleAi(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertEquals(3, move.to)
        assertTrue(game.areas[move.to].dice < game.areas[move.from].dice)
    }

    @Test
    fun noValidMovesReturnsNull() {
        val game = aiGame()
        game.areas[1].dice = 1
        game.areas[4].dice = 1

        assertNull(ExampleAi(FixedAiRandom(0)).chooseMove(game))
        assertNull(DefaultAi(FixedAiRandom(0)).chooseMove(game))
        assertNull(DefensiveAi().chooseMove(game))
    }
}

private fun aiGame(): DicewarsGame {
    val game = DicewarsGame()
    game.pmax = 2
    game.turnOrder[0] = 0
    game.turnOrder[1] = 1
    game.turnIndex = 0

    game.areas[1].size = 5
    game.areas[1].owner = 0
    game.areas[1].dice = 5
    game.areas[1].adjacentAreas[2] = 1
    game.areas[1].adjacentAreas[3] = 1

    game.areas[2].size = 5
    game.areas[2].owner = 1
    game.areas[2].dice = 3
    game.areas[2].adjacentAreas[1] = 1
    game.areas[2].adjacentAreas[4] = 1

    game.areas[3].size = 5
    game.areas[3].owner = 1
    game.areas[3].dice = 4
    game.areas[3].adjacentAreas[1] = 1

    game.areas[4].size = 5
    game.areas[4].owner = 0
    game.areas[4].dice = 2
    game.areas[4].adjacentAreas[2] = 1

    game.setAreaTc(0)
    game.setAreaTc(1)
    return game
}

private class FixedAiRandom(private val value: Int) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return value % bound
    }
}
