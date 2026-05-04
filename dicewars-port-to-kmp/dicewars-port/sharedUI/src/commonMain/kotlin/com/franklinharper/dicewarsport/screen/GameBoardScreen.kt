package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.*
import com.franklinharper.dicewarsport.presentation.components.MapRenderer
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold
import kotlinx.coroutines.delay

@Composable
fun GameBoardScreen(state: GameUiState, onAction: (GameAction) -> Unit, title: String) = ScreenScaffold(
    title = title,
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
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
internal fun ColumnScope.Board(state: GameUiState, onAction: (GameAction) -> Unit) {
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
