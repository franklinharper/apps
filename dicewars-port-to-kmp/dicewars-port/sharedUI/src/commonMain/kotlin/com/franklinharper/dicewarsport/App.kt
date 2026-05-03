package com.franklinharper.dicewarsport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.presentation.components.MapRenderer
import com.franklinharper.dicewarsport.theme.AppTheme
import kotlinx.coroutines.delay

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) = AppTheme(onThemeChanged) {
    val random = remember { IncrementingRandomSource() }
    val reducer = remember { GameReducer(random) }
    var state by remember {
        mutableStateOf(
            GameUiState(
                screen = DicewarsScreen.Loading,
                game = DicewarsGame().also { it.makeMap(random) },
            ),
        )
    }

    DicewarsApp(
        state = state,
        onAction = { action -> state = reducer.reduce(state, action) },
    )
}

@Composable
fun DicewarsApp(
    state: GameUiState,
    onAction: (GameAction) -> Unit = {},
) {
    when (state.screen) {
        DicewarsScreen.Loading -> LoadingScreen(onAction)
        DicewarsScreen.Title -> TitleScreen(onAction)
        DicewarsScreen.MapPreview -> MapPreviewScreen(state, onAction)
        DicewarsScreen.HumanTurn -> GameBoardScreen(state, onAction, title = "Your turn")
        DicewarsScreen.AiTurn -> GameBoardScreen(state, onAction, title = "AI turn")
        DicewarsScreen.Battle -> BattleScreen(state, onAction)
        DicewarsScreen.Supply -> SupplyScreen(state, onAction)
        DicewarsScreen.GameOver -> GameOverScreen(onAction)
        DicewarsScreen.Win -> WinScreen(onAction)
        DicewarsScreen.History -> HistoryScreen(state, onAction)
    }
}

fun routedDicewarsScreens(): Set<DicewarsScreen> = setOf(
    DicewarsScreen.Loading,
    DicewarsScreen.Title,
    DicewarsScreen.MapPreview,
    DicewarsScreen.HumanTurn,
    DicewarsScreen.AiTurn,
    DicewarsScreen.Battle,
    DicewarsScreen.Supply,
    DicewarsScreen.GameOver,
    DicewarsScreen.Win,
    DicewarsScreen.History,
)

@Composable
fun LoadingScreen(onAction: (GameAction) -> Unit) {
    LaunchedEffect(Unit) {
        delay(2_000)
        onAction(GameAction.LoadingFinished)
    }

    ScreenScaffold("Loading") {
        Text("Loading...")
    }
}

@Composable
fun TitleScreen(onAction: (GameAction) -> Unit) = ScreenScaffold("Dicewars") {
    Button(onClick = { onAction(GameAction.StartPressed) }) { Text("Start") }
    Button(onClick = { onAction(GameAction.StartSpectate) }) { Text("Spectate") }
}

@Composable
fun MapPreviewScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold("Play this board?") {
    Board(state, onAction)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onAction(GameAction.AcceptMap) }) { Text("Play") }
        Button(onClick = { onAction(GameAction.RejectMap) }) { Text("New board") }
    }
}

@Composable
fun GameBoardScreen(state: GameUiState, onAction: (GameAction) -> Unit, title: String) = ScreenScaffold(title) {
    Board(state, onAction)
    if (state.screen == DicewarsScreen.HumanTurn) {
        Button(onClick = { onAction(GameAction.EndTurn) }) { Text("End turn") }
    } else {
        Button(onClick = { onAction(GameAction.AiStep) }) { Text("AI step") }
    }
}

@Composable
fun BattleScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold("Battle") {
    val roll = state.pendingBattleRoll
    Text("${state.selectedFrom} attacks ${state.selectedTo}")
    if (roll != null) {
        Text("${roll.attackerTotal} vs ${roll.defenderTotal}")
        Text(if (roll.success) "Success" else "Failure")
    }
    Button(onClick = { onAction(GameAction.BattleAnimationFinished) }) { Text("Finish battle") }
}

@Composable
fun SupplyScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold("Supply") {
    Text("Player ${state.game.currentPlayer()} receives supply")
    Button(onClick = { onAction(GameAction.SupplyAnimationFinished) }) { Text("Continue") }
}

@Composable
fun GameOverScreen(onAction: (GameAction) -> Unit) = ScreenScaffold("Game Over") {
    Button(onClick = { onAction(GameAction.OpenHistory) }) { Text("History") }
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("Title") }
}

@Composable
fun WinScreen(onAction: (GameAction) -> Unit) = ScreenScaffold("You Win") {
    Button(onClick = { onAction(GameAction.OpenHistory) }) { Text("History") }
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("Title") }
}

@Composable
fun HistoryScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold("History") {
    Text("${state.game.history.size} recorded events")
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("Title") }
}

@Composable
private fun ScreenScaffold(title: String, content: ColumnScopeContent) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            content()
        }
    }
}

private typealias ColumnScopeContent = @Composable () -> Unit

@Composable
private fun Board(state: GameUiState, onAction: (GameAction) -> Unit) {
    val map = remember(state.game) { state.game.toRenderMap() }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        MapRenderer(
            map = map,
            cellWidth = 10f,
            cellHeight = 7f,
            fontSize = 8f,
            highlightedTerritories = listOfNotNull(state.selectedFrom?.minus(1), state.selectedTo?.minus(1)).toSet(),
            attackFromTerritory = state.selectedFrom?.minus(1),
            onTerritoryClick = { territoryIndex -> onAction(GameAction.TerritoryClicked(territoryIndex + 1)) },
        )
    }
    Spacer(Modifier.height(8.dp))
}

private class IncrementingRandomSource : RandomSource {
    private var next = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = next % bound
        next++
        return value
    }
}
