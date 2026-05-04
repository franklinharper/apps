package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(2, next.game.players[0].maxConnectedAreaCount)
        assertEquals(1, next.game.players[1].maxConnectedAreaCount)
    }

    @Test
    fun humanTurnReceivesReinforcementsAndAdvancesToNextPlayerOnEndTurn() {
        val state = reducer().reduce(turnState(DicewarsScreen.HumanTurn), GameAction.EndTurn)

        assertEquals(DicewarsScreen.AiTurn, state.screen)
        assertEquals(1, state.game.currentPlayer())
        assertEquals(5, state.game.areas[1].dice)
    }

    @Test
    fun aiTurnResolvesAttackOrFinishesTurnImmediately() {
        val next = reducer(ai = FixedMoveAi(Move(2, 1))).reduce(turnState(DicewarsScreen.AiTurn, currentPlayer = 1), GameAction.AiStep)
        val finished = reducer(ai = FixedMoveAi(null)).reduce(turnState(DicewarsScreen.AiTurn, currentPlayer = 1), GameAction.AiStep)

        assertEquals(DicewarsScreen.AiTurn, next.screen)
        assertEquals(DicewarsScreen.HumanTurn, finished.screen)
        assertEquals(0, finished.game.currentPlayer())
    }

    @Test
    fun immediateAttackResolutionTransitionsToWinOrGameOver() {
        val gameOverGame = uiGame(areas = mapOf(
            1 to AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(2)),
            2 to AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = adj(1)),
        ), turnIndex = 1)
        val gameOver = reducer(ai = FixedMoveAi(Move(2, 1))).reduce(
            GameUiState(screen = DicewarsScreen.AiTurn, game = gameOverGame),
            GameAction.AiStep,
        )
        assertEquals(DicewarsScreen.GameOver, gameOver.screen)

        val winGame = uiGame(areas = mapOf(
            1 to AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2)),
            2 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 3)),
            3 to AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(2)),
        ))
        val win = reducer().reduce(
            reducer().reduce(GameUiState(screen = DicewarsScreen.HumanTurn, game = winGame), GameAction.TerritoryClicked(1)),
            GameAction.TerritoryClicked(2),
        )
        assertEquals(DicewarsScreen.Win, win.screen)
    }

    @Test
    fun winAndGameOverReturnToTitle() {
        val fromWin = reducer().reduce(initialUiState(screen = DicewarsScreen.Win), GameAction.BackToTitle)
        val fromGameOver = reducer().reduce(initialUiState(screen = DicewarsScreen.GameOver), GameAction.BackToTitle)

        assertEquals(DicewarsScreen.Title, fromWin.screen)
        assertEquals(DicewarsScreen.Title, fromGameOver.screen)
    }

    @Test
    fun spectateModeReusesExistingScreensAndDoesNotAddEleventhScreen() {
        val state = reducer().reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartSpectate)

        assertTrue(state.spectateMode)
        assertEquals(DicewarsScreen.MapPreview, state.screen)
    }
}

private fun adj(vararg ids: Int): List<Int> {
    val list = MutableList(DicewarsGame.AREA_MAX) { 0 }
    for (id in ids) if (id in list.indices) list[id] = 1
    return list
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
    val game = uiGame().copy(user = user, turnOrder = listOf(currentPlayer, 1, 2, 3, 4, 5, 6, 7).take(8))
    return GameUiState(screen = DicewarsScreen.MapPreview, game = game)
}

private fun turnState(screen: DicewarsScreen, currentPlayer: Int = 0): GameUiState {
    val game = uiGame().copy(turnIndex = currentPlayer)
    return GameUiState(screen = screen, game = game)
}

private fun uiGame(
    areas: Map<Int, AreaData> = mapOf(
        1 to AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2)),
        2 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 3)),
        3 to AreaData(size = 5, owner = 1, dice = 1, adjacentAreas = adj(2)),
    ),
    turnIndex: Int = 0,
): DicewarsGame {
    val game = DicewarsGame(
        pmax = 2,
        user = 0,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = turnIndex,
        areas = List(DicewarsGame.AREA_MAX) { i -> areas[i] ?: AreaData() },
    )
    return game.setAreaTc(0).setAreaTc(1)
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
