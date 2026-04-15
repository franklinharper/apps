package com.franklinharper.wordlecoach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.franklinharper.wordlecoach.domain.CoachingState
import com.franklinharper.wordlecoach.domain.CoachingStep
import com.franklinharper.wordlecoach.domain.WordShapeStats

@Composable
fun CoachingScreen(
    state: CoachingState,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar(state = state, onBack = onBack, onForward = onForward)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentStep) {
                CoachingStep.BeforeFirstGuess -> BeforeFirstGuessStep()
            }
        }
    }
}

@Composable
private fun BeforeFirstGuessStep() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Step 1 — Before your first guess",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Text(
            text = "Word shapes in the answer list (C = consonant, V = vowel, Y = vowel)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
        )
        ShapeHeader()
        HorizontalDivider()
        LazyColumn {
            items(WordShapeStats.stats) { stat ->
                ShapeRow(
                    shape = stat.shape,
                    count = stat.count,
                    percentage = stat.percentage,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun ShapeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Shape",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Words",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = "  %",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ShapeRow(shape: String, count: Int, percentage: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = shape,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun NavigationBar(
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                enabled = state.canGoBack,
            ) {
                Text("← Back")
            }
            Text(
                text = "Step ${state.currentStepIndex + 1} of ${state.steps.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onForward,
                enabled = state.canGoForward,
            ) {
                Text("Forward →")
            }
        }
    }
}
