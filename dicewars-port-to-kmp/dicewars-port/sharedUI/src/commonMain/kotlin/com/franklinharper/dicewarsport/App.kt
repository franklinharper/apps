package com.franklinharper.dicewarsport

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
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
    debugPreferences: DebugPreferences = NoOpDebugPreferences(),
) = AppTheme(onThemeChanged) {
    val random = remember { KotlinRandomSource() }
    val reducer = remember { GameReducer(random, debugPreferences = debugPreferences) }
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
        DicewarsScreen.Debug -> DebugScreen(state, onAction)
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
    DicewarsScreen.Debug,
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
fun TitleScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    "Dicewars",
    showDebugIcon = state.debugMode,
    onGoToDebug = { onAction(GameAction.GoToDebug) },
    onTitleTap = { onAction(GameAction.TitleTapped) },
) {
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
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
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
fun GameOverScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Game Over",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
) {
    AnimatedRobot(modifier = Modifier.weight(1f).fillMaxWidth())
    Button(
        onClick = { onAction(GameAction.BackToTitle) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("New Game") }
}

@Composable
fun WinScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "You Win",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
) {
    AnimatedTrophy(modifier = Modifier.weight(1f).fillMaxWidth())
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("New Game") }
}

@Composable
fun DebugScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Debug Menu",
    showBackButton = true,
    onBack = { onAction(GameAction.BackToTitle) },
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.Win)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Win Screen") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.GameOver)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Game Over Screen") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.HumanTurn)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Human Turn") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.AiTurn)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show AI Turn") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.Title)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Title Screen") }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onAction(GameAction.DisableDebugMode) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text("Disable Debug Mode") }
    }
}

@Composable
private fun AnimatedRobot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "robot")

    // Jump bounce
    val jumpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jumpOffset",
    )

    // Left arm wave
    val leftArmAngle by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "leftArm",
    )

    // Right arm wave (offset phase)
    val rightArmAngle by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rightArm",
    )

    // Antenna bob
    val antennaBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "antennaBob",
    )

    // Eye blink
    val blink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, delayMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )

    // Entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "entrance",
    )

    val bodyColor = Color(0xFF78909C)
    val bodyDark = Color(0xFF546E7A)
    val headColor = Color(0xFF90A4AE)
    val accentColor = Color(0xFF42A5F5)
    val eyeColor = Color(0xFF1769AA)
    val cheekColor = Color(0xFFEF9A9A)
    val antennaColor = Color(0xFFFF7043)

    Canvas(
        modifier = modifier.scale(entranceScale),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val scale = minOf(w, h) / 300f

        // Scale helper
        fun s(v: Float) = v * scale
        fun s(v: Int) = v * scale

        // Ground shadow (scales with jump)
        val shadowScale = 1f + (jumpOffset / 80f)
        drawOval(
            color = Color(0x22000000),
            topLeft = Offset(cx - s(70) * shadowScale, h * 0.88f),
            size = androidx.compose.ui.geometry.Size(s(140) * shadowScale, s(16)),
        )

        val jumpPx = jumpOffset * scale
        val bodyTop = h * 0.42f + s(10) + jumpPx
        val bodyLeft = cx - s(55)
        val bodyW = s(110)
        val bodyH = s(100)

        // Legs
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(30), bodyTop + bodyH),
            size = androidx.compose.ui.geometry.Size(s(22), s(40)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx + s(8), bodyTop + bodyH),
            size = androidx.compose.ui.geometry.Size(s(22), s(40)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )

        // Feet
        drawRoundRect(
            color = Color(0xFF455A64),
            topLeft = Offset(cx - s(38), bodyTop + bodyH + s(28)),
            size = androidx.compose.ui.geometry.Size(s(35), s(14)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )
        drawRoundRect(
            color = Color(0xFF455A64),
            topLeft = Offset(cx + s(3), bodyTop + bodyH + s(28)),
            size = androidx.compose.ui.geometry.Size(s(35), s(14)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )

        // Body
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyLeft, bodyTop),
            size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(16)),
        )
        // Body panel
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(20), bodyTop + s(20)),
            size = androidx.compose.ui.geometry.Size(s(40), s(30)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
        )
        // Panel light (blinking heart)
        val heartAlpha = 0.5f + 0.5f * kotlin.math.sin(System.currentTimeMillis() / 300.0).toFloat()
        drawCircle(
            color = accentColor.copy(alpha = heartAlpha),
            radius = s(8),
            center = Offset(cx, bodyTop + s(35)),
        )
        // Panel dots
        drawCircle(
            color = Color(0xFF66BB6A),
            radius = s(4),
            center = Offset(cx - s(10), bodyTop + s(25)),
        )
        drawCircle(
            color = Color(0xFFFFA726),
            radius = s(4),
            center = Offset(cx + s(10), bodyTop + s(25)),
        )

        // Left arm (waves)
        val leftArmPivotX = bodyLeft
        val leftArmPivotY = bodyTop + s(15)
        rotate(leftArmAngle, Offset(leftArmPivotX, leftArmPivotY)) {
            drawRoundRect(
                color = bodyDark,
                topLeft = Offset(leftArmPivotX - s(15), leftArmPivotY),
                size = androidx.compose.ui.geometry.Size(s(18), s(55)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
            )
            // Hand
            drawCircle(
                color = headColor,
                radius = s(10),
                center = Offset(leftArmPivotX - s(6), leftArmPivotY + s(58)),
            )
        }

        // Right arm (waves)
        val rightArmPivotX = bodyLeft + bodyW
        val rightArmPivotY = bodyTop + s(15)
        rotate(rightArmAngle, Offset(rightArmPivotX, rightArmPivotY)) {
            drawRoundRect(
                color = bodyDark,
                topLeft = Offset(rightArmPivotX - s(3), rightArmPivotY),
                size = androidx.compose.ui.geometry.Size(s(18), s(55)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(8)),
            )
            // Hand
            drawCircle(
                color = headColor,
                radius = s(10),
                center = Offset(rightArmPivotX + s(6), rightArmPivotY + s(58)),
            )
        }

        // Neck
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - s(12), bodyTop - s(12)),
            size = androidx.compose.ui.geometry.Size(s(24), s(18)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(6)),
        )

        // Head
        val headCenterY = bodyTop - s(55)
        drawRoundRect(
            color = headColor,
            topLeft = Offset(cx - s(50), headCenterY - s(40)),
            size = androidx.compose.ui.geometry.Size(s(100), s(80)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(20)),
        )

        // Face plate
        drawRoundRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(cx - s(40), headCenterY - s(28)),
            size = androidx.compose.ui.geometry.Size(s(80), s(52)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(14)),
        )

        // Eyes
        val eyeY = headCenterY - s(8)
        val leftEyeX = cx - s(18)
        val rightEyeX = cx + s(18)
        // Eye whites
        drawCircle(color = Color.White, radius = s(12), center = Offset(leftEyeX, eyeY))
        drawCircle(color = Color.White, radius = s(12), center = Offset(rightEyeX, eyeY))
        // Pupils
        drawOval(
            color = eyeColor,
            topLeft = Offset(leftEyeX - s(6), eyeY - s(8 * blink)),
            size = androidx.compose.ui.geometry.Size(s(12), s(16 * blink)),
        )
        drawOval(
            color = eyeColor,
            topLeft = Offset(rightEyeX - s(6), eyeY - s(8 * blink)),
            size = androidx.compose.ui.geometry.Size(s(12), s(16 * blink)),
        )
        // Eye shine
        drawCircle(color = Color.White, radius = s(3), center = Offset(leftEyeX + s(3), eyeY - s(3)))
        drawCircle(color = Color.White, radius = s(3), center = Offset(rightEyeX + s(3), eyeY - s(3)))

        // Smile
        val smilePath = Path().apply {
            moveTo(cx - s(16), headCenterY + s(12))
            quadraticBezierTo(cx, headCenterY + s(22), cx + s(16), headCenterY + s(12))
        }
        drawPath(
            path = smilePath,
            color = eyeColor,
            style = Stroke(width = s(4), cap = StrokeCap.Round),
        )

        // Cheeks
        drawCircle(
            color = cheekColor.copy(alpha = 0.5f),
            radius = s(8),
            center = Offset(cx - s(32), headCenterY + s(6)),
        )
        drawCircle(
            color = cheekColor.copy(alpha = 0.5f),
            radius = s(8),
            center = Offset(cx + s(32), headCenterY + s(6)),
        )

        // Ears
        drawCircle(
            color = bodyColor,
            radius = s(12),
            center = Offset(cx - s(50), headCenterY - s(5)),
        )
        drawCircle(
            color = bodyColor,
            radius = s(12),
            center = Offset(cx + s(50), headCenterY - s(5)),
        )

        // Antenna stem
        drawLine(
            color = bodyDark,
            start = Offset(cx, headCenterY - s(40)),
            end = Offset(cx, headCenterY - s(60) + antennaBob),
            strokeWidth = s(5),
            cap = StrokeCap.Round,
        )
        // Antenna ball
        drawCircle(
            color = antennaColor,
            radius = s(10),
            center = Offset(cx, headCenterY - s(65) + antennaBob),
        )
        // Antenna glow
        drawCircle(
            color = antennaColor.copy(alpha = 0.3f),
            radius = s(16),
            center = Offset(cx, headCenterY - s(65) + antennaBob),
        )
    }
}

@Composable
private fun AnimatedTrophy(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")

    // Gentle pulsing scale
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophyScale",
    )

    // Sparkle rotation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
        ),
        label = "sparkleRotation",
    )

    val sparklePhases = remember { List(8) { it * (Math.PI.toFloat() / 4f) } }
    val twinkleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
        ),
        label = "twinkle",
    )

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "entrance",
    )

    val gold = Color(0xFFFFD700)
    val goldLight = Color(0xFFFFEC80)
    val goldDark = Color(0xFFDAA520)
    val goldDarker = Color(0xFFB8860B)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Sparkle ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(entranceScale),
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * 0.42f

            rotate(sparkleRotation, center) {
                for (i in sparklePhases.indices) {
                    val angle = sparklePhases[i]
                    val twinkle = (kotlin.math.sin((twinkleTime + angle).toDouble()) * 0.5 + 0.5).toFloat()
                    val sparkleAlpha = 0.3f + twinkle * 0.7f
                    val sparkleSize = 4.dp.toPx() + twinkle * 8.dp.toPx()
                    val x = center.x + radius * kotlin.math.cos(angle)
                    val y = center.y + radius * kotlin.math.sin(angle)

                    drawLine(
                        color = gold.copy(alpha = sparkleAlpha),
                        start = Offset(x - sparkleSize, y),
                        end = Offset(x + sparkleSize, y),
                        strokeWidth = sparkleSize * 0.3f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = gold.copy(alpha = sparkleAlpha),
                        start = Offset(x, y - sparkleSize),
                        end = Offset(x, y + sparkleSize),
                        strokeWidth = sparkleSize * 0.3f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Trophy + glow
        Canvas(
            modifier = Modifier
                .scale(scale * entranceScale)
                .fillMaxSize(0.6f),
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f

            // Golden glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55FFD700), Color(0x00FFD700)),
                    center = Offset(cx, h * 0.4f),
                    radius = w * 0.5f,
                ),
                center = Offset(cx, h * 0.4f),
                radius = w * 0.5f,
            )

            // Cup body
            val cupTop = h * 0.08f
            val cupBottom = h * 0.52f
            val cupLeftTop = cx - w * 0.35f
            val cupRightTop = cx + w * 0.35f
            val cupLeftBottom = cx - w * 0.18f
            val cupRightBottom = cx + w * 0.18f

            val cupPath = Path().apply {
                moveTo(cupLeftTop, cupTop)
                lineTo(cupRightTop, cupTop)
                lineTo(cupRightBottom, cupBottom)
                // Rounded bottom
                quadraticBezierTo(cx, cupBottom + h * 0.06f, cupLeftBottom, cupBottom)
                close()
            }
            drawPath(cupPath, gold)

            // Cup highlight (left side)
            val highlightPath = Path().apply {
                moveTo(cupLeftTop + w * 0.06f, cupTop)
                lineTo(cupLeftTop + w * 0.14f, cupTop)
                lineTo(cupLeftBottom + w * 0.06f, cupBottom - h * 0.04f)
                quadraticBezierTo(cx - w * 0.15f, cupBottom + h * 0.02f, cupLeftBottom + w * 0.02f, cupBottom - h * 0.02f)
                close()
            }
            drawPath(highlightPath, goldLight)

            // Cup shadow (right side)
            val shadowPath = Path().apply {
                moveTo(cupRightTop - w * 0.1f, cupTop)
                lineTo(cupRightTop, cupTop)
                lineTo(cupRightBottom, cupBottom)
                quadraticBezierTo(cx + w * 0.05f, cupBottom + h * 0.04f, cx + w * 0.08f, cupBottom - h * 0.02f)
                close()
            }
            drawPath(shadowPath, goldDark)

            // Left handle
            val handlePath = Path().apply {
                moveTo(cupLeftTop, cupTop + h * 0.06f)
                cubicTo(
                    cx - w * 0.55f, cupTop + h * 0.06f,
                    cx - w * 0.55f, cupTop + h * 0.32f,
                    cupLeftTop, cupTop + h * 0.32f,
                )
            }
            drawPath(
                handlePath,
                gold,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
            )

            // Right handle
            val handleRPath = Path().apply {
                moveTo(cupRightTop, cupTop + h * 0.06f)
                cubicTo(
                    cx + w * 0.55f, cupTop + h * 0.06f,
                    cx + w * 0.55f, cupTop + h * 0.32f,
                    cupRightTop, cupTop + h * 0.32f,
                )
            }
            drawPath(
                handleRPath,
                gold,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
            )

            // Rim at top of cup
            drawLine(
                color = goldLight,
                start = Offset(cupLeftTop - w * 0.02f, cupTop),
                end = Offset(cupRightTop + w * 0.02f, cupTop),
                strokeWidth = h * 0.025f,
                cap = StrokeCap.Round,
            )

            // Star on cup
            val starCx = cx
            val starCy = cupTop + (cupBottom - cupTop) * 0.4f
            val starOuter = w * 0.1f
            val starInner = w * 0.04f
            val starPath = Path().apply {
                for (i in 0 until 5) {
                    val outerAngle = Math.toRadians((-90.0 + i * 72.0))
                    val innerAngle = Math.toRadians((-90.0 + 36.0 + i * 72.0))
                    val ox = starCx + starOuter * kotlin.math.cos(outerAngle.toFloat())
                    val oy = starCy + starOuter * kotlin.math.sin(outerAngle.toFloat())
                    val ix = starCx + starInner * kotlin.math.cos(innerAngle.toFloat())
                    val iy = starCy + starInner * kotlin.math.sin(innerAngle.toFloat())
                    if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                    lineTo(ix, iy)
                }
                close()
            }
            drawPath(starPath, Color(0xFFFFFFF0))

            // Stem
            val stemTop = cupBottom + h * 0.01f
            val stemBottom = cupBottom + h * 0.16f
            drawRect(
                color = goldDark,
                topLeft = Offset(cx - w * 0.035f, stemTop),
                size = androidx.compose.ui.geometry.Size(w * 0.07f, stemBottom - stemTop),
            )
            // Stem highlight
            drawRect(
                color = gold,
                topLeft = Offset(cx - w * 0.035f, stemTop),
                size = androidx.compose.ui.geometry.Size(w * 0.035f, stemBottom - stemTop),
            )

            // Base top
            val baseTop = stemBottom
            val baseBottom = baseTop + h * 0.04f
            drawRect(
                color = goldDark,
                topLeft = Offset(cx - w * 0.2f, baseTop),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, baseBottom - baseTop),
            )
            // Base highlight strip
            drawRect(
                color = gold,
                topLeft = Offset(cx - w * 0.2f, baseTop),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, (baseBottom - baseTop) * 0.4f),
            )
            // Base bottom
            drawRect(
                color = goldDarker,
                topLeft = Offset(cx - w * 0.24f, baseBottom),
                size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.025f),
            )
        }
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
private fun ScreenScaffold(
    title: String,
    showBackButton: Boolean = false,
    showSoundToggle: Boolean = false,
    soundEnabled: Boolean = true,
    showDebugIcon: Boolean = false,
    onBack: (() -> Unit)? = null,
    onToggleSound: (() -> Unit)? = null,
    onGoToDebug: (() -> Unit)? = null,
    onTitleTap: (() -> Unit)? = null,
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
                val titleModifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (onTitleTap != null) Modifier.clickable(onClick = onTitleTap) else Modifier,
                    )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = titleModifier,
                )
                // Debug icon (left of sound toggle)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showDebugIcon && onGoToDebug != null) {
                        IconButton(onClick = onGoToDebug) {
                            Text("🐛", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (showSoundToggle && onToggleSound != null) {
                        IconButton(onClick = onToggleSound) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Sound on" else "Sound off",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
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
