package com.franklinharper.dicewarsport

data class GameUiState(
    val screen: DicewarsScreen,
    val game: DicewarsGame,
    val selectedFrom: Int? = null,
    val selectedTo: Int? = null,
    val spectateMode: Boolean = false,
    val selectedPlayerCount: Int = game.pmax,
    val soundEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val titleTapCount: Int = 0,
    val titleTapTimestamp: Long = 0L,
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
    data object ToggleSound : GameAction
    data object TitleTapped : GameAction
    data object GoToDebug : GameAction
    data class ShowDebugScreen(val screen: DicewarsScreen) : GameAction
    data object DisableDebugMode : GameAction
}

class GameReducer(
    private val random: RandomSource,
    private val aiStrategies: Map<Int, AiStrategy> = emptyMap(),
    private val debugPreferences: DebugPreferences = NoOpDebugPreferences(),
) {
    companion object {
        private const val TAP_THRESHOLD = 5
        private const val TAP_WINDOW_MS = 3000L
    }
    data class Result(
        val state: GameUiState,
        val soundEvents: List<SoundEvent> = emptyList(),
    )

    fun reduce(state: GameUiState, action: GameAction): Result = when (action) {
        GameAction.LoadingFinished -> {
            val debugMode = debugPreferences.isDebugMode()
            Result(state.copy(screen = DicewarsScreen.Title, debugMode = debugMode))
        }
        is GameAction.SelectPlayerCount -> Result(
            state.copy(selectedPlayerCount = action.count),
            listOf(SoundEvent.BUTTON),
        )
        GameAction.StartPressed -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview))
        }
        GameAction.StartSpectate -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview, spectateMode = true))
        }
        GameAction.AcceptMap -> {
            val newScreen = turnScreenFor(state.game, state.spectateMode)
            val sounds = mutableListOf<SoundEvent>()
            if (newScreen == DicewarsScreen.HumanTurn) sounds.add(SoundEvent.MY_TURN)
            Result(state.copy(screen = newScreen), sounds)
        }
        GameAction.RejectMap -> {
            val newGame = DicewarsGame.generate(state.game.pmax, random)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview))
        }
        is GameAction.TerritoryClicked -> onTerritoryClicked(state, action.territoryId)
        GameAction.EndTurn -> onTurnFinished(state)
        GameAction.AiStep -> onAiStep(state)
        GameAction.BackToTitle -> Result(state.copy(screen = DicewarsScreen.Title, selectedFrom = null, selectedTo = null))
        GameAction.ToggleSound -> Result(state.copy(soundEnabled = !state.soundEnabled))
        GameAction.TitleTapped -> onTitleTapped(state)
        GameAction.GoToDebug -> Result(state.copy(screen = DicewarsScreen.Debug))
        is GameAction.ShowDebugScreen -> onShowDebugScreen(state, action.screen)
        GameAction.DisableDebugMode -> {
            debugPreferences.setDebugMode(false)
            Result(state.copy(debugMode = false, screen = DicewarsScreen.Title))
        }
    }

    private fun onTerritoryClicked(state: GameUiState, territoryId: Int): Result {
        if (state.screen != DicewarsScreen.HumanTurn) return Result(state)

        val selectedFrom = state.selectedFrom
        if (selectedFrom == null) {
            val area = state.game.areas.getOrNull(territoryId) ?: return Result(state)
            return if (area.size > 0 && area.owner == state.game.currentPlayer() && area.dice > 1) {
                Result(state.copy(selectedFrom = territoryId), listOf(SoundEvent.CLICK))
            } else {
                Result(state)
            }
        }

        if (territoryId == selectedFrom) return Result(state.copy(selectedFrom = null, selectedTo = null))
        if (!state.game.isLegalAttack(selectedFrom, territoryId)) return Result(state)

        val roll = rollBattle(
            attackerDiceCount = state.game.areas[selectedFrom].dice,
            defenderDiceCount = state.game.areas[territoryId].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(selectedFrom, territoryId, roll)

        val battleSounds = listOf(SoundEvent.DICE, if (roll.success) SoundEvent.SUCCESS else SoundEvent.FAIL)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            val terminalSounds = battleSounds + terminalScreenSound(terminalScreen)
            return Result(
                state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null),
                terminalSounds,
            )
        }

        return Result(
            state.copy(
                game = newGame,
                screen = turnScreenFor(newGame, state.spectateMode),
                selectedFrom = null,
                selectedTo = null,
            ),
            battleSounds,
        )
    }

    private fun onAiStep(state: GameUiState): Result {
        if (state.screen != DicewarsScreen.AiTurn) return Result(state)
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

        val battleSounds = listOf(SoundEvent.DICE, if (roll.success) SoundEvent.SUCCESS else SoundEvent.FAIL)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            val terminalSounds = battleSounds + terminalScreenSound(terminalScreen)
            return Result(
                state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null),
                terminalSounds,
            )
        }

        return Result(
            state.copy(
                game = newGame,
                screen = turnScreenFor(newGame, state.spectateMode),
                selectedFrom = null,
                selectedTo = null,
            ),
            battleSounds,
        )
    }

    private fun onTurnFinished(state: GameUiState): Result {
        val player = state.game.currentPlayer()
        var game = state.game.startSupply(player)
        while (true) {
            val (newGame, areaNumber) = game.supplyOneDie(player, random)
            game = newGame
            if (areaNumber == null) break
        }
        game = game.nextPlayer()
        val newScreen = turnScreenFor(game, state.spectateMode)
        val sounds = mutableListOf<SoundEvent>()
        if (newScreen == DicewarsScreen.HumanTurn) sounds.add(SoundEvent.MY_TURN)
        return Result(
            state.copy(
                game = game,
                screen = newScreen,
                selectedFrom = null,
                selectedTo = null,
            ),
            sounds,
        )
    }

    private fun terminalScreenOrNull(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen? {
        if (game.players[game.user].maxConnectedAreaCount == 0 && !spectateMode) return DicewarsScreen.GameOver
        val activePlayers = (0 until game.pmax).count { game.players[it].maxConnectedAreaCount > 0 }
        if (activePlayers == 1) return if (spectateMode) DicewarsScreen.GameOver else DicewarsScreen.Win
        return null
    }

    private fun terminalScreenSound(screen: DicewarsScreen): SoundEvent = when (screen) {
        DicewarsScreen.Win -> SoundEvent.WIN
        else -> SoundEvent.GAME_OVER
    }

    private fun turnScreenFor(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen =
        if (!spectateMode && game.currentPlayer() == game.user) DicewarsScreen.HumanTurn else DicewarsScreen.AiTurn

    private fun onTitleTapped(state: GameUiState): Result {
        val now = System.currentTimeMillis()
        val withinWindow = (now - state.titleTapTimestamp) < TAP_WINDOW_MS
        val newCount = if (withinWindow) state.titleTapCount + 1 else 1
        val newDebugMode = if (newCount >= TAP_THRESHOLD) !state.debugMode else state.debugMode
        if (newDebugMode != state.debugMode) {
            debugPreferences.setDebugMode(newDebugMode)
        }
        return Result(
            state.copy(
                debugMode = newDebugMode,
                titleTapCount = if (newDebugMode != state.debugMode) 0 else newCount,
                titleTapTimestamp = if (newDebugMode != state.debugMode) 0L else now,
            ),
        )
    }

    private fun onShowDebugScreen(state: GameUiState, targetScreen: DicewarsScreen): Result {
        val needsGame = targetScreen in setOf(
            DicewarsScreen.HumanTurn, DicewarsScreen.AiTurn,
        )
        val game = if (needsGame) DicewarsGame.generate(state.selectedPlayerCount, random) else state.game
        return Result(state.copy(screen = targetScreen, game = game, selectedFrom = null, selectedTo = null))
    }
}
