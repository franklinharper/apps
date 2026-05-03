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
    data object SupplyAnimationFinished : GameAction
    data object OpenHistory : GameAction
    data object BackToTitle : GameAction
    data object StartSpectate : GameAction
}

class GameReducer(
    private val random: RandomSource,
    private val aiStrategies: Map<Int, AiStrategy> = emptyMap(),
) {
    fun reduce(state: GameUiState, action: GameAction): GameUiState = when (action) {
        GameAction.LoadingFinished -> state.copy(screen = DicewarsScreen.Title)
        is GameAction.SelectPlayerCount -> {
            state.game.pmax = action.count
            state.copy(selectedPlayerCount = action.count)
        }
        GameAction.StartPressed -> {
            state.game.pmax = state.selectedPlayerCount
            state.game.makeMap(random)
            state.copy(screen = DicewarsScreen.MapPreview)
        }
        GameAction.StartSpectate -> state.copy(screen = DicewarsScreen.MapPreview, spectateMode = true)
        GameAction.AcceptMap -> state.copy(screen = turnScreenFor(state.game, state.spectateMode))
        GameAction.RejectMap -> {
            state.game.makeMap(random)
            state.copy(screen = DicewarsScreen.MapPreview)
        }
        is GameAction.TerritoryClicked -> onTerritoryClicked(state, action.territoryId)
        GameAction.EndTurn -> state.copy(screen = DicewarsScreen.Supply, selectedFrom = null, selectedTo = null)
        GameAction.AiStep -> onAiStep(state)
        GameAction.SupplyAnimationFinished -> onSupplyFinished(state)
        GameAction.OpenHistory -> state.copy(screen = DicewarsScreen.History)
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

        val roll = state.game.rollBattle(
            attackerDiceCount = state.game.areas[selectedFrom].dice,
            defenderDiceCount = state.game.areas[territoryId].dice,
            random = random,
        )
        state.game.resolveBattle(selectedFrom, territoryId, roll)

        val terminalScreen = terminalScreenOrNull(state.game, state.spectateMode)
        if (terminalScreen != null) {
            return state.copy(screen = terminalScreen, selectedFrom = null, selectedTo = null)
        }

        return state.copy(
            screen = turnScreenFor(state.game, state.spectateMode),
            selectedFrom = null,
            selectedTo = null,
        )
    }

    private fun onAiStep(state: GameUiState): GameUiState {
        if (state.screen != DicewarsScreen.AiTurn) return state
        val player = state.game.currentPlayer()
        val strategy = aiStrategies[player] ?: DefaultAi(random)
        val move = strategy.chooseMove(state.game) ?: return state.copy(screen = DicewarsScreen.Supply)
        if (!state.game.isLegalAttack(move.from, move.to, player)) return state.copy(screen = DicewarsScreen.Supply)

        val roll = state.game.rollBattle(
            attackerDiceCount = state.game.areas[move.from].dice,
            defenderDiceCount = state.game.areas[move.to].dice,
            random = random,
        )
        state.game.resolveBattle(move.from, move.to, roll)

        val terminalScreen = terminalScreenOrNull(state.game, state.spectateMode)
        if (terminalScreen != null) {
            return state.copy(screen = terminalScreen, selectedFrom = null, selectedTo = null)
        }

        return state.copy(
            screen = turnScreenFor(state.game, state.spectateMode),
            selectedFrom = null,
            selectedTo = null,
        )
    }

    private fun onSupplyFinished(state: GameUiState): GameUiState {
        state.game.nextPlayer()
        return state.copy(screen = turnScreenFor(state.game, state.spectateMode), selectedFrom = null, selectedTo = null)
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
