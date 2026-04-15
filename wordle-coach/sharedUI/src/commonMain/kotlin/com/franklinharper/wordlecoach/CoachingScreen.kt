package com.franklinharper.wordlecoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franklinharper.wordlecoach.domain.*

// ── Wordle tile colours ────────────────────────────────────────────────────

private val ColorCorrect = Color(0xFF538D4E)   // green
private val ColorPresent = Color(0xFFB59F3B)   // yellow
private val ColorAbsent  = Color(0xFF787C7E)   // grey
private val ColorTileText = Color.White

@Composable
fun CoachingScreen(
    state: CoachingState,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationRow(state = state, onBack = onBack, onForward = onForward)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val step = state.currentStep) {
                is CoachingStep.BeforeFirstGuess -> BeforeFirstGuessStep()
                is CoachingStep.AfterGuess       -> AfterGuessStep(step)
            }
        }
    }
}

// ── Step 1: Before first guess ─────────────────────────────────────────────

@Composable
private fun BeforeFirstGuessStep() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Step 1 — Before your first guess",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = "C/V shape distribution across the ${WordShapeStats.stats.sumOf { it.count }} answer words  (Y counts as vowel)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp),
        )
        ShapeTableHeader()
        HorizontalDivider()
        LazyColumn {
            items(WordShapeStats.stats) { stat ->
                ShapeRow(stat)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun ShapeTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Shape", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("Words", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        Text("  %", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun ShapeRow(stat: ShapeStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stat.shape,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stat.count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End,
        )
        Text(
            text = "${"%.1f".format(stat.percentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

// ── Step 2+: After a guess ─────────────────────────────────────────────────

@Composable
private fun AfterGuessStep(step: CoachingStep.AfterGuess) {
    val totalAnswers = WordShapeStats.stats.sumOf { it.count }
    val pct = step.remainingAnswers.size.toDouble() / totalAnswers * 100.0

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Step ${step.guessNumber + 1} — After guess ${step.guessNumber}: ${step.guess.word}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Coloured tile row
        GuessTileRow(
            guess = step.guess,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
        )

        // Remaining-answers summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${step.remainingAnswers.size} of $totalAnswers answers still possible",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${"%.1f".format(pct)}% of the answer list remains",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Shape breakdown of remaining answers
        if (step.remainingAnswers.isNotEmpty()) {
            Text(
                text = "Shape distribution of remaining answers",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val shapeStats = remainingShapeStats(step.remainingAnswers)
            LazyColumn {
                items(shapeStats) { stat ->
                    ShapeRow(stat)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** Computes shape stats for an arbitrary subset of words (same logic as [WordShapeStats]). */
private fun remainingShapeStats(words: List<String>): List<ShapeStat> {
    val total = words.size.toDouble()
    return words
        .groupingBy { WordShape.of(it) }
        .eachCount()
        .map { (shape, count) -> ShapeStat(shape, count, count / total * 100.0) }
        .sortedByDescending { it.percentage }
}

// ── Guess tile row ─────────────────────────────────────────────────────────

@Composable
private fun GuessTileRow(guess: CompletedGuess, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        guess.tiles.forEach { tile ->
            GuessTile(tile)
        }
    }
}

@Composable
private fun GuessTile(tile: GuessedTile) {
    val bg = when (tile.result) {
        LetterResult.Correct -> ColorCorrect
        LetterResult.Present -> ColorPresent
        LetterResult.Absent  -> ColorAbsent
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(bg, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.letter.toString(),
            color = ColorTileText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ── Bottom navigation row ──────────────────────────────────────────────────

@Composable
private fun NavigationRow(
    state: CoachingState,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, enabled = state.canGoBack) {
                Text("← Back")
            }
            Text(
                text = "Step ${state.currentStepIndex + 1} of ${state.steps.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onForward, enabled = state.canGoForward) {
                Text("Forward →")
            }
        }
    }
}
