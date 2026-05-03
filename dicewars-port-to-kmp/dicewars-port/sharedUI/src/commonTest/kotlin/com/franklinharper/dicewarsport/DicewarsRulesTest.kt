package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DicewarsRulesTest {

    @Test
    fun legalAttackRequiresOwnedSourceWithMoreThanOneDieEnemyTargetAndAdjacency() {
        val game = rulesGame()

        assertTrue(game.isLegalAttack(from = 1, to = 2, player = 0))
        assertFalse(game.isLegalAttack(from = 2, to = 1, player = 0), "attacker must own source")

        game.areas[1].dice = 1
        assertFalse(game.isLegalAttack(from = 1, to = 2, player = 0), "source dice must be > 1")
        game.areas[1].dice = 3

        game.areas[2].owner = 0
        assertFalse(game.isLegalAttack(from = 1, to = 2, player = 0), "target must be enemy")
        game.areas[2].owner = 1

        game.areas[1].adjacentAreas[2] = 0
        game.areas[2].adjacentAreas[1] = 0
        assertFalse(game.isLegalAttack(from = 1, to = 2, player = 0), "target must be adjacent")
    }

    @Test
    fun battleRollContainsDiceTotalsAndSuccess() {
        val roll = DicewarsGame().rollBattle(attackerDiceCount = 2, defenderDiceCount = 2, FixedRandomSource(5, 0, 2, 1))

        assertEquals(listOf(6, 1), roll.attackerDice)
        assertEquals(listOf(3, 2), roll.defenderDice)
        assertEquals(7, roll.attackerTotal)
        assertEquals(5, roll.defenderTotal)
        assertTrue(roll.success)
    }

    @Test
    fun attackerWinsOnlyWhenAttackerTotalIsGreaterThanDefenderTotal() {
        val game = DicewarsGame()

        assertTrue(game.rollBattle(1, 1, FixedRandomSource(1, 0)).success)
        assertFalse(game.rollBattle(1, 1, FixedRandomSource(0, 0)).success)
        assertFalse(game.rollBattle(1, 1, FixedRandomSource(0, 1)).success)
    }

    @Test
    fun attackerLossSetsSourceDiceToOneAndRecordsHistory() {
        val game = rulesGame(sourceDice = 4, targetDice = 5)
        val roll = BattleRoll(listOf(1), listOf(6), attackerTotal = 1, defenderTotal = 6, success = false)

        game.resolveBattle(from = 1, to = 2, roll = roll)

        assertEquals(1, game.areas[1].dice)
        assertEquals(5, game.areas[2].dice)
        assertEquals(1, game.areas[2].owner)
        assertEquals(HistoryData(from = 1, to = 2, result = 0), game.history.single())
    }

    @Test
    fun attackerWinTransfersOwnerAndDiceAndUpdatesConnectedAreaCounts() {
        val game = rulesGame(sourceDice = 4, targetDice = 2)
        val roll = BattleRoll(listOf(6), listOf(1), attackerTotal = 6, defenderTotal = 1, success = true)

        game.resolveBattle(from = 1, to = 2, roll = roll)

        assertEquals(1, game.areas[1].dice)
        assertEquals(3, game.areas[2].dice)
        assertEquals(0, game.areas[2].owner)
        assertEquals(2, game.players[0].maxConnectedAreaCount)
        assertEquals(0, game.players[1].maxConnectedAreaCount)
        assertEquals(HistoryData(from = 1, to = 2, result = 1), game.history.single())
    }

    @Test
    fun supplyIsCappedAtStockMaxAndOnlyAffectsOwnedAreasBelowEightDice() {
        val game = rulesGame(sourceDice = 7, targetDice = 7)
        game.areas[3].size = 5
        game.areas[3].owner = 0
        game.areas[3].dice = 8
        game.players[0].stock = DicewarsGame.STOCK_MAX - 1

        val stock = game.startSupply(player = 0)
        assertEquals(DicewarsGame.STOCK_MAX, stock)

        val suppliedArea = game.supplyOneDie(player = 0, random = FixedRandomSource(0))

        assertEquals(1, suppliedArea)
        assertEquals(8, game.areas[1].dice)
        assertEquals(8, game.areas[3].dice)
        assertEquals(7, game.areas[2].dice, "enemy area is not supplied")
        assertEquals(DicewarsGame.STOCK_MAX - 1, game.players[0].stock)
        assertEquals(HistoryData(from = 1, to = 0, result = 0), game.history.single())
    }

    @Test
    fun nextPlayerSkipsEliminatedPlayers() {
        val game = DicewarsGame()
        game.pmax = 3
        game.turnOrder[0] = 0
        game.turnOrder[1] = 1
        game.turnOrder[2] = 2
        game.turnIndex = 0
        game.players[1].maxConnectedAreaCount = 0
        game.players[2].maxConnectedAreaCount = 1

        assertEquals(2, game.nextPlayer())
        assertEquals(2, game.currentPlayer())
    }
}

private fun rulesGame(sourceDice: Int = 3, targetDice: Int = 2): DicewarsGame {
    val game = DicewarsGame()
    game.pmax = 2
    game.turnOrder[0] = 0
    game.turnOrder[1] = 1
    game.turnIndex = 0

    game.areas[1].size = 5
    game.areas[1].owner = 0
    game.areas[1].dice = sourceDice
    game.areas[1].adjacentAreas[2] = 1

    game.areas[2].size = 5
    game.areas[2].owner = 1
    game.areas[2].dice = targetDice
    game.areas[2].adjacentAreas[1] = 1

    game.setAreaTc(0)
    game.setAreaTc(1)
    return game
}

private class FixedRandomSource(private vararg val values: Int) : RandomSource {
    private var index = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = values.getOrElse(index) { 0 } % bound
        index++
        return value
    }
}
