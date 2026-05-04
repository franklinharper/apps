package com.franklinharper.dicewarsport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.presentation.components.MapRenderer
import com.franklinharper.dicewarsport.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    soundPlayer: SoundPlayer = NoOpSoundPlayer(),
) = AppTheme(onThemeChanged) {
    val random = remember { KotlinRandomSource() }
    val reducer = remember { GameReducer(random) }
    var state by remember {
        mutableStateOf(GameUiState(screen = DicewarsScreen.Loading, game = DicewarsGame()))
    }

    DicewarsApp(
        state = state,
        onAction = { action ->
            val result = reducer.reduce(state, action)
            state = result.state
            if (state.soundEnabled) {
                result.soundEvents.forEach { soundPlayer.play(it) }
            }
        },
    )
}

@Composable
fun DicewarsApp(
    state: GameUiState,
    onAction: (GameAction) -> Unit = {},
) {
    when (state.screen) {
        DicewarsScreen.Loading -> LoadingScreen(onAction)
        DicewarsScreen.Title -> TitleScreen(state, onAction)
        DicewarsScreen.MapPreview -> MapPreviewScreen(state, onAction)
        DicewarsScreen.HumanTurn -> GameBoardScreen(state, onAction, title = "Your turn")
        DicewarsScreen.AiTurn -> GameBoardScreen(state, onAction, title = "AI turn")
        DicewarsScreen.GameOver -> GameOverScreen(state, onAction)
        DicewarsScreen.Win -> WinScreen(state, onAction)
    }
}

fun routedDicewarsScreens(): Set<DicewarsScreen> = setOf(
    DicewarsScreen.Loading,
    DicewarsScreen.Title,
    DicewarsScreen.MapPreview,
    DicewarsScreen.HumanTurn,
    DicewarsScreen.AiTurn,
    DicewarsScreen.GameOver,
    DicewarsScreen.Win,
)

@Composable
fun LoadingScreen(onAction: (GameAction) -> Unit) {
    LaunchedEffect(Unit) {
        delay(500)
        onAction(GameAction.LoadingFinished)
    }

    ScreenScaffold("Loading") {
    }
}

@Composable
fun TitleScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold("Dicewars") {
    Text(
        text = "How many players?",
        style = MaterialTheme.typography.titleMedium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (count in 2..8) {
            val selected = count == state.selectedPlayerCount
            Button(
                onClick = { onAction(GameAction.SelectPlayerCount(count)) },
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = if (selected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text("$count")
            }
        }
    }
    Button(onClick = { onAction(GameAction.StartPressed) }) { Text("Start") }
}

@Composable
fun MapPreviewScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Play this board?",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
) {
    Board(state, onAction)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onAction(GameAction.AcceptMap) }) { Text("Play") }
        Button(onClick = { onAction(GameAction.RejectMap) }) { Text("New board") }
    }
}

@Composable
fun GameBoardScreen(state: GameUiState, onAction: (GameAction) -> Unit, title: String) = ScreenScaffold(
    title = title,
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
) {
    Board(state, onAction)
    Text(
        text = "Reinforcements",
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    PlayerStatusBar(state.game)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.screen == DicewarsScreen.HumanTurn) {
            Text(
                text = "1. Click your area. 2. Click neighbor to attack.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LaunchedEffect(state.game) {
                delay(300)
                onAction(GameAction.AiStep)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(GameColors.getPlayerColor(state.game.currentPlayer())),
            )
            Spacer(Modifier.size(8.dp))
            Text("bot's turn")
        }
    }
    if (state.screen == DicewarsScreen.HumanTurn) {
        Button(onClick = { onAction(GameAction.EndTurn) }) { Text("End turn") }
    }
}

@Composable
fun GameOverScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Game Over",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
) {
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("Title") }
}

@Composable
fun WinScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "You Win",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
) {
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("Title") }
}

@Composable
private fun PlayerStatusBar(game: DicewarsGame) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (player in game.turnOrder.take(game.pmax)) {
            val isCurrentPlayer = player == game.currentPlayer()
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCurrentPlayer) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(GameColors.getPlayerColor(player)),
                    )
                    Text(
                        text = "${game.players[player].maxConnectedAreaCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrentPlayer) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    )
                }
                Text(
                    text = if (game.players[player].stock > 0) "${game.players[player].stock}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentPlayer) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    minLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ScreenScaffold(
    title: String,
    showBackButton: Boolean = false,
    showSoundToggle: Boolean = false,
    soundEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
    onToggleSound: (() -> Unit)? = null,
    content: ColumnScopeContent,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showBackButton && onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (showSoundToggle && onToggleSound != null) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Sound on" else "Sound off",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            content()
        }
    }
}

private typealias ColumnScopeContent = @Composable ColumnScope.() -> Unit

@Composable
private fun ColumnScope.Board(state: GameUiState, onAction: (GameAction) -> Unit) {
    val map = remember(state.game) { state.game.toRenderMap() }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidthPx = constraints.maxWidth.toFloat()
        val availableHeightPx = constraints.maxHeight.toFloat()
        val cellWidthFromWidth = availableWidthPx / (HexGrid.GRID_WIDTH + 0.5f)
        val cellWidthFromHeight = availableHeightPx / HexGrid.GRID_HEIGHT
        val cellWidth = minOf(cellWidthFromWidth, cellWidthFromHeight)
        val cellHeight = cellWidth * 2f / 3f
        val fontSize = with(density) { (cellWidth * 0.8f).toSp() }.value.coerceIn(6f, 18f)

        MapRenderer(
            map = map,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            fontSize = fontSize,
            highlightedTerritories = listOfNotNull(state.selectedFrom?.minus(1), state.selectedTo?.minus(1)).toSet(),
            attackFromTerritory = state.selectedFrom?.minus(1),
            onTerritoryClick = { territoryIndex -> onAction(GameAction.TerritoryClicked(territoryIndex + 1)) },
        )
    }
    Spacer(Modifier.height(8.dp))
}

private class KotlinRandomSource(
    private val random: Random = Random.Default,
) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return random.nextInt(bound)
    }
}
