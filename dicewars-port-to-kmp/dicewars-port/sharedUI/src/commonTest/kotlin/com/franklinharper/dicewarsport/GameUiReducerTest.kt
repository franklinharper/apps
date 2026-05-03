package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameUiReducerTest {

    @Test
    fun loadingTransitionsToTitle() {
        val state = reducer().reduce(initialUiState(), GameAction.LoadingFinished)

        assertEquals(DicewarsScreen.Title, state.screen)
    }

    @Test
    fun titleTransitionsToMapPreview() {
        val state = reducer().reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartPressed)

        assertEquals(DicewarsScreen.MapPreview, state.screen)
    }

    @Test
    fun selectedPlayerCountIsUsedWhenGameStarts() {
        val reducer = reducer()
        val selected = reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.SelectPlayerCount(4))
        val started = reducer.reduce(selected, GameAction.StartPressed)

        assertEquals(4, selected.selectedPlayerCount)
        assertEquals(4, started.game.pmax)
        assertEquals(DicewarsScreen.MapPreview, started.screen)
    }

    @Test
    fun mapPreviewTransitionsToHumanOrAiTurn() {
        val human = reducer().reduce(previewState(user = 0, currentPlayer = 0), GameAction.AcceptMap)
        val ai = reducer().reduce(previewState(user = 0, currentPlayer = 1), GameAction.AcceptMap)

        assertEquals(DicewarsScreen.HumanTurn, human.screen)
        assertEquals(DicewarsScreen.AiTurn, ai.screen)
    }

    @Test
    fun humanTurnResolvesAttackAndStaysOnTurnAfterTwoLegalClicks() {
        val reducer = reducer()
        val selected = reducer.reduce(turnState(DicewarsScreen.HumanTurn), GameAction.TerritoryClicked(1))
        val next = reducer.reduce(selected, GameAction.TerritoryClicked(2))

        assertEquals(DicewarsScreen.HumanTurn, next.screen)
        assertEquals(null, next.selectedFrom)
        assertEquals(null, next.selectedTo)
        assertEquals(0, next.game.areas[2].owner)
        assertEquals(3, next.game.areas[2].dice)
        assertEquals(1, next.game.areas[1].dice)
    }

    @Test
    fun humanTurnTransitionsToSupplyOnEndTurn() {
        val state = reducer().reduce(turnState(DicewarsScreen.HumanTurn), GameAction.EndTurn)

        assertEquals(DicewarsScreen.Supply, state.screen)
    }

    @Test
    fun aiTurnResolvesAttackOrTransitionsToSupply() {
        val next = reducer(ai = FixedMoveAi(Move(2, 1))).reduce(turnState(DicewarsScreen.AiTurn, currentPlayer = 1), GameAction.AiStep)
        val supply = reducer(ai = FixedMoveAi(null)).reduce(turnState(DicewarsScreen.AiTurn, currentPlayer = 1), GameAction.AiStep)

        assertEquals(DicewarsScreen.AiTurn, next.screen)
        assertEquals(DicewarsScreen.Supply, supply.screen)
    }

    @Test
    fun immediateAttackResolutionTransitionsToWinOrGameOver() {
        val gameOverGame = uiGame()
        gameOverGame.areas[1].dice = 1
        gameOverGame.areas[2].dice = 8
        gameOverGame.turnIndex = 1
        val gameOver = reducer(ai = FixedMoveAi(Move(2, 1))).reduce(
            GameUiState(screen = DicewarsScreen.AiTurn, game = gameOverGame),
            GameAction.AiStep,
        )
        assertEquals(DicewarsScreen.GameOver, gameOver.screen)

        val winGame = uiGame()
        winGame.areas[3].owner = 0
        winGame.setAreaTc(0)
        winGame.setAreaTc(1)
        val win = reducer().reduce(
            reducer().reduce(GameUiState(screen = DicewarsScreen.HumanTurn, game = winGame), GameAction.TerritoryClicked(1)),
            GameAction.TerritoryClicked(2),
        )
        assertEquals(DicewarsScreen.Win, win.screen)
    }

    @Test
    fun supplyAnimationTransitionsToNextActivePlayerTurn() {
        val state = reducer().reduce(turnState(DicewarsScreen.Supply), GameAction.SupplyAnimationFinished)

        assertEquals(DicewarsScreen.AiTurn, state.screen)
    }

    @Test
    fun winAndGameOverOpenHistoryAndHistoryReturnsTitle() {
        val fromWin = reducer().reduce(initialUiState(screen = DicewarsScreen.Win), GameAction.OpenHistory)
        val fromGameOver = reducer().reduce(initialUiState(screen = DicewarsScreen.GameOver), GameAction.OpenHistory)
        val title = reducer().reduce(fromWin, GameAction.BackToTitle)

        assertEquals(DicewarsScreen.History, fromWin.screen)
        assertEquals(DicewarsScreen.History, fromGameOver.screen)
        assertEquals(DicewarsScreen.Title, title.screen)
    }

    @Test
    fun spectateModeReusesExistingScreensAndDoesNotAddEleventhScreen() {
        val state = reducer().reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartSpectate)

        assertTrue(state.spectateMode)
        assertEquals(DicewarsScreen.MapPreview, state.screen)
        assertEquals(9, DicewarsScreen.entries.size)
        assertFalse(DicewarsScreen.entries.any { it.name.contains("Spectate", ignoreCase = true) })
    }
}

private fun reducer(ai: AiStrategy = FixedMoveAi(null)): GameReducer = GameReducer(
    random = UiFixedRandom(),
    aiStrategies = mapOf(1 to ai),
)

private fun initialUiState(screen: DicewarsScreen = DicewarsScreen.Loading): GameUiState = GameUiState(
    screen = screen,
    game = uiGame(),
)

private fun previewState(user: Int, currentPlayer: Int): GameUiState {
    val game = uiGame()
    game.user = user
    game.turnOrder[0] = currentPlayer
    game.turnIndex = 0
    return GameUiState(screen = DicewarsScreen.MapPreview, game = game)
}

private fun turnState(screen: DicewarsScreen, currentPlayer: Int = 0): GameUiState {
    val game = uiGame()
    game.turnIndex = currentPlayer
    return GameUiState(screen = screen, game = game)
}

private fun uiGame(): DicewarsGame {
    val game = DicewarsGame()
    game.pmax = 2
    game.user = 0
    game.turnOrder[0] = 0
    game.turnOrder[1] = 1
    game.turnIndex = 0

    game.areas[1].size = 5
    game.areas[1].owner = 0
    game.areas[1].dice = 4
    game.areas[1].adjacentAreas[2] = 1

    game.areas[2].size = 5
    game.areas[2].owner = 1
    game.areas[2].dice = 2
    game.areas[2].adjacentAreas[1] = 1
    game.areas[2].adjacentAreas[3] = 1

    game.areas[3].size = 5
    game.areas[3].owner = 1
    game.areas[3].dice = 1
    game.areas[3].adjacentAreas[2] = 1

    game.setAreaTc(0)
    game.setAreaTc(1)
    return game
}

private class FixedMoveAi(private val move: Move?) : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? = move
}

private class UiFixedRandom : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return 0
    }
}
