package com.franklinharper.dicewarsport

data class GameUiState(
    val screen: DicewarsScreen,
    val game: DicewarsGame,
    val selectedFrom: Int? = null,
    val selectedTo: Int? = null,
    val spectateMode: Boolean = false,
    val selectedPlayerCount: Int = game.pmax,
)

sealed interface GameAction {
    data object LoadingFinished : GameAction
    data class SelectPlayerCount(val count: Int) : GameAction
    data object StartPressed : GameAction
    data object AcceptMap : GameAction
    data object RejectMap : GameAction
    data class TerritoryClicked(val territoryId: Int) : GameAction
    data object EndTurn : GameAction
    data object AiStep : GameAction
    data object BackToTitle : GameAction
    data object StartSpectate : GameAction
}

class GameReducer(
    private val random: RandomSource,
    private val aiStrategies: Map<Int, AiStrategy> = emptyMap(),
) {
    fun reduce(state: GameUiState, action: GameAction): GameUiState = when (action) {
        GameAction.LoadingFinished -> state.copy(screen = DicewarsScreen.Title)
        is GameAction.SelectPlayerCount -> state.copy(selectedPlayerCount = action.count)
        GameAction.StartPressed -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            state.copy(game = newGame, screen = DicewarsScreen.MapPreview)
        }
        GameAction.StartSpectate -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            state.copy(game = newGame, screen = DicewarsScreen.MapPreview, spectateMode = true)
        }
        GameAction.AcceptMap -> state.copy(screen = turnScreenFor(state.game, state.spectateMode))
        GameAction.RejectMap -> {
            val newGame = DicewarsGame.generate(state.game.pmax, random)
            state.copy(game = newGame, screen = DicewarsScreen.MapPreview)
        }
        is GameAction.TerritoryClicked -> onTerritoryClicked(state, action.territoryId)
        GameAction.EndTurn -> onTurnFinished(state)
        GameAction.AiStep -> onAiStep(state)
        GameAction.BackToTitle -> state.copy(screen = DicewarsScreen.Title, selectedFrom = null, selectedTo = null)
    }

    private fun onTerritoryClicked(state: GameUiState, territoryId: Int): GameUiState {
        if (state.screen != DicewarsScreen.HumanTurn) return state

        val selectedFrom = state.selectedFrom
        if (selectedFrom == null) {
            val area = state.game.areas.getOrNull(territoryId) ?: return state
            return if (area.size > 0 && area.owner == state.game.currentPlayer() && area.dice > 1) {
                state.copy(selectedFrom = territoryId)
            } else {
                state
            }
        }

        if (territoryId == selectedFrom) return state.copy(selectedFrom = null, selectedTo = null)
        if (!state.game.isLegalAttack(selectedFrom, territoryId)) return state

        val roll = rollBattle(
            attackerDiceCount = state.game.areas[selectedFrom].dice,
            defenderDiceCount = state.game.areas[territoryId].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(selectedFrom, territoryId, roll)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            return state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null)
        }

        return state.copy(
            game = newGame,
            screen = turnScreenFor(newGame, state.spectateMode),
            selectedFrom = null,
            selectedTo = null,
        )
    }

    private fun onAiStep(state: GameUiState): GameUiState {
        if (state.screen != DicewarsScreen.AiTurn) return state
        val player = state.game.currentPlayer()
        val strategy = aiStrategies[player] ?: DefaultAi(random)
        val move = strategy.chooseMove(state.game) ?: return onTurnFinished(state)
        if (!state.game.isLegalAttack(move.from, move.to, player)) return onTurnFinished(state)

        val roll = rollBattle(
            attackerDiceCount = state.game.areas[move.from].dice,
            defenderDiceCount = state.game.areas[move.to].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(move.from, move.to, roll)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            return state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null)
        }

        return state.copy(
            game = newGame,
            screen = turnScreenFor(newGame, state.spectateMode),
            selectedFrom = null,
            selectedTo = null,
        )
    }

    private fun onTurnFinished(state: GameUiState): GameUiState {
        val player = state.game.currentPlayer()
        var game = state.game.startSupply(player)
        while (true) {
            val (newGame, areaNumber) = game.supplyOneDie(player, random)
            game = newGame
            if (areaNumber == null) break
        }
        game = game.nextPlayer()
        return state.copy(
            game = game,
            screen = turnScreenFor(game, state.spectateMode),
            selectedFrom = null,
            selectedTo = null,
        )
    }

    private fun terminalScreenOrNull(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen? {
        if (game.players[game.user].maxConnectedAreaCount == 0 && !spectateMode) return DicewarsScreen.GameOver
        val activePlayers = (0 until game.pmax).count { game.players[it].maxConnectedAreaCount > 0 }
        if (activePlayers == 1) return if (spectateMode) DicewarsScreen.GameOver else DicewarsScreen.Win
        return null
    }

    private fun turnScreenFor(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen =
        if (!spectateMode && game.currentPlayer() == game.user) DicewarsScreen.HumanTurn else DicewarsScreen.AiTurn
}
