package com.example.boardgametimer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.GameState
import com.example.boardgametimer.model.PhaseType
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameTimerScreen(
    gameState: GameState,
    gameConfiguration: GameConfiguration,
    onNextPlayer: () -> Unit,
    onPauseResume: () -> Unit,
    onShowOptions: () -> Unit,
    onEndGame: () -> Unit,
    onDiceClick: () -> Unit = {},
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Calculate circle size based on screen size
    val circleSize = min(screenWidth.value, screenHeight.value * 0.6f).dp

    // Animation for pointer rotation - fix rotation to not go full circle on last player
    val rotationAngle = remember { Animatable(0f) }

    LaunchedEffect(gameState.currentPlayerIndex, gameConfiguration.numberOfPlayers) {
        // Calculate target angle for text (original position)
        val targetAngle = (gameState.currentPlayerIndex * 360f / gameConfiguration.numberOfPlayers)

        // Always rotate clockwise - don't take shortest path for player transitions
        val currentAngle = rotationAngle.value
        val newTargetAngle = if (targetAngle < currentAngle % 360f && gameState.currentPlayerIndex == 0) {
            // Handle wrap-around from last player to first player - continue clockwise
            currentAngle + (360f - (currentAngle % 360f)) + targetAngle
        } else {
            // Normal clockwise progression
            val baseAngle = (currentAngle / 360f).toInt() * 360f
            baseAngle + targetAngle
        }

        rotationAngle.animateTo(
            targetValue = newTargetAngle,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.weight(1f))

        // Game Timer Circle with Pointer
        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            // Draw the pointer and circle
            TimerCircleWithPointer(
                gameState = gameState,
                gameConfiguration = gameConfiguration,
                circleSize = circleSize,
                pointerRotation = rotationAngle.value,
                onCircleClick = onNextPlayer,
                onDiceClick = onDiceClick,
                onNextPhase = onNextPlayer
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings Button
            IconButton(
                onClick = onShowOptions,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Pause/Resume Button
            IconButton(
                onClick = onPauseResume,
                modifier = Modifier.size(48.dp),
                enabled = gameState.isGameRunning
            ) {
                Icon(
                    imageVector = if (gameState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (gameState.isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // START/END Button - Change text based on game state
        Button(
            onClick = if (gameState.isGameRunning) onEndGame else {
                // Start new game logic will be handled in MainActivity
                onEndGame
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gameState.isGameRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (gameState.isGameRunning) "END GAME" else "START GAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Undo Button (if undo is available)
        if (gameState.isGameRunning) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onUndo) {
                Text("Undo Last Action")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TimerCircleWithPointer(
    gameState: GameState,
    gameConfiguration: GameConfiguration,
    circleSize: androidx.compose.ui.unit.Dp,
    pointerRotation: Float,
    onCircleClick: () -> Unit,
    onDiceClick: () -> Unit = {},
    onNextPhase: () -> Unit = {}
) {
    // Use millisecond precision for smooth animation with current phase duration
    val currentPhaseDuration = gameConfiguration.getCurrentPhaseDuration(gameState.currentPhaseIndex)
    val progress = gameState.timeRemainingMillis.toFloat() / (currentPhaseDuration * 1000f)

    // Colors for the circle (bright blue to dark blue)
    val brightBlue = Color(0xFF1E88E5)
    val darkBlue = Color(0xFF0D47A1)
    val currentColor = androidx.compose.ui.graphics.lerp(darkBlue, brightBlue, progress)
    val pointerColor = Color(0xFF81D4FA)

    // Calculate rotation for text orientation based on configuration
    val textRotation = if (gameConfiguration.keepCounterAlignmentFixed) 0f else pointerRotation

    Box(
        modifier = Modifier.size(circleSize),
        contentAlignment = Alignment.Center
    ) {
        // Canvas for drawing circle and pointer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable { onCircleClick() }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 40.dp.toPx() // More space for the arrow

            // Draw the timer circle
            drawTimerCircle(
                center = center,
                radius = radius,
                progress = progress,
                color = currentColor
            )

            // Draw the pointer flipped 180 degrees from text position
            drawArrowPointer(
                center = center,
                radius = radius,
                rotation = pointerRotation + 180f, // Flip arrow by 180 degrees
                color = pointerColor,
                playerCount = gameConfiguration.numberOfPlayers
            )
        }

        // Timer text in the center - use original rotation (not flipped)
        Box(
            modifier = Modifier
                .size(circleSize * 0.6f)
                .then(
                    if (!gameConfiguration.keepCounterAlignmentFixed) {
                        Modifier.graphicsLayer {
                            rotationZ = pointerRotation // Use original rotation for text
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            val currentPhase = gameConfiguration.turnPhases.getOrNull(gameState.currentPhaseIndex)

            if (currentPhase?.phaseType == PhaseType.DICE_THROW) {
                // Show dice display instead of timer text
                DiceDisplay(
                    diceType = currentPhase.diceType,
                    diceCount = currentPhase.diceCount,
                    diceResult = gameState.diceResult,
                    isAnimating = gameState.isDiceAnimating,
                    circleSize = circleSize,
                    onDiceClick = onDiceClick,
                    onNextPhase = onNextPhase
                )
            } else {
                // Show normal timer display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime(gameState.timeRemainingSeconds),
                        fontSize = (circleSize.value / 8).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = gameConfiguration.getPlayerName(gameState.currentPlayerIndex),
                        fontSize = (circleSize.value / 16).sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Round counter title
                    Text(
                        text = "Round ${gameState.roundCount + 1}",
                        fontSize = (circleSize.value / 20).sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )

                    // Phase information
                    if (gameConfiguration.getTotalPhases() > 1) {
                        Text(
                            text = gameConfiguration.getCurrentPhaseName(gameState.currentPhaseIndex),
                            fontSize = (circleSize.value / 20).sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Phase ${gameState.currentPhaseIndex + 1}/${gameConfiguration.getTotalPhases()}",
                            fontSize = (circleSize.value / 24).sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    if (gameState.isPaused) {
                        Text(
                            text = "PAUSED",
                            fontSize = (circleSize.value / 20).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawTimerCircle(
    center: Offset,
    radius: Float,
    progress: Float,
    color: Color
) {
    val strokeWidth = 20.dp.toPx()

    // Draw background circle
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // Draw progress arc
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawPointer(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color,
    playerCount: Int
) {
    val pointerWidth = 360f / playerCount.toFloat()
    val pointerLength = radius + 30.dp.toPx()
    val pointerThickness = 15.dp.toPx()

    // Calculate pointer end position
    val angleRad = Math.toRadians((rotation - 90).toDouble())
    val endX = center.x + cos(angleRad).toFloat() * pointerLength
    val endY = center.y + sin(angleRad).toFloat() * pointerLength

    // Draw pointer as a thick line
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = pointerThickness,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawArrowPointer(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color,
    playerCount: Int
) {
    val angleWidth = 360f / playerCount.toFloat()
    val outerRadius = radius + 35.dp.toPx()
    val innerRadius = radius + 15.dp.toPx()

    // Calculate start and end angles for the arc
    val startAngle = rotation - angleWidth / 2f
    val endAngle = rotation + angleWidth / 2f

    // Draw the wide arc pointer
    drawArc(
        color = color,
        startAngle = startAngle - 90f, // Offset by 90 degrees to start at top
        sweepAngle = angleWidth,
        useCenter = false,
        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
        size = Size(outerRadius * 2, outerRadius * 2),
        style = Stroke(width = (outerRadius - innerRadius), cap = StrokeCap.Round)
    )
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}
