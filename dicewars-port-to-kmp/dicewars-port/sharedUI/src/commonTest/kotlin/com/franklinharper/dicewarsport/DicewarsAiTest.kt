package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DicewarsAiTest {

    @Test
    fun aiStrategiesNeverReturnIllegalMoves() {
        val strategies = listOf(AlwaysAttackWhenStrongerBot(FixedAiRandom(0)), TargetTheLeader(FixedAiRandom(0)), CautiousBot())
        for (strategy in strategies) {
            val game = aiGame()
            val move = strategy.chooseMove(game)
            assertNotNull(move, strategy::class.simpleName ?: "strategy")
            assertTrue(game.isLegalAttack(move.from, move.to), "${strategy::class.simpleName} returned illegal $move")
        }
    }

    @Test
    fun alwaysAttackWhenStrongerBotAttacksOnlyWeakerAdjacentEnemyAreas() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[2] = it[2].copy(dice = it[1].dice)
            },
        )

        val move = AlwaysAttackWhenStrongerBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertEquals(3, move.to)
        assertTrue(game.areas[move.to].dice < game.areas[move.from].dice)
    }

    @Test
    fun noValidMovesReturnsNull() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[1] = it[1].copy(dice = 1)
                it[4] = it[4].copy(dice = 1)
            },
        )

        assertNull(AlwaysAttackWhenStrongerBot(FixedAiRandom(0)).chooseMove(game))
        assertNull(TargetTheLeader(FixedAiRandom(0)).chooseMove(game))
        assertNull(CautiousBot().chooseMove(game))
    }
}

private fun adj(vararg ids: Int): List<Int> {
    val list = MutableList(DicewarsGame.AREA_MAX) { 0 }
    for (id in ids) if (id in list.indices) list[id] = 1
    return list
}

private fun aiGame(): DicewarsGame {
    val game = DicewarsGame(
        pmax = 2,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = adj(2, 3))
                2 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1, 4))
                3 -> AreaData(size = 5, owner = 1, dice = 4, adjacentAreas = adj(1))
                4 -> AreaData(size = 5, owner = 0, dice = 2, adjacentAreas = adj(2))
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

private class FixedAiRandom(private val value: Int) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return value % bound
    }
}
