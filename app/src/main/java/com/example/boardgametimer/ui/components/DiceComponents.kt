package com.example.boardgametimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boardgametimer.model.DiceType
import com.example.boardgametimer.model.DiceResult
import kotlin.math.*

@Composable
fun DiceDisplay(
    diceType: DiceType,
    diceCount: Int,
    diceResult: DiceResult?,
    isAnimating: Boolean,
    circleSize: Dp,
    onDiceClick: () -> Unit,
    onNextPhase: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(circleSize * 0.6f)
            .clip(CircleShape)
            .clickable {
                if (diceResult != null && !isAnimating) {
                    // After dice are thrown, clicking should move to next phase
                    onNextPhase()
                } else {
                    // Before throwing, clicking should throw dice
                    onDiceClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (diceResult != null) {
            if (diceCount == 1) {
                // Single die - show large die with polygon and number
                SingleDiceDisplay(
                    diceType = diceType,
                    value = diceResult.values.first(),
                    isAnimating = isAnimating,
                    size = circleSize * 0.5f
                )
            } else {
                // Multiple dice - show individual results with sum below
                MultipleDiceDisplay(
                    diceType = diceType,
                    diceResult = diceResult,
                    isAnimating = isAnimating,
                    circleSize = circleSize
                )
            }
        } else {
            // Waiting to throw - show instruction
            WaitingToThrowDisplay(diceType, diceCount, circleSize)
        }
    }
}

@Composable
private fun SingleDiceDisplay(
    diceType: DiceType,
    value: Int,
    isAnimating: Boolean,
    size: Dp
) {
    // Draw the polygon shape for the die with the number inside
    DicePolygon(
        diceType = diceType,
        size = size,
        isAnimating = isAnimating,
        value = value
    )
}

@Composable
private fun MultipleDiceDisplay(
    diceType: DiceType,
    diceResult: DiceResult,
    isAnimating: Boolean,
    circleSize: Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calculate dice size based on actual available space
        // The DiceDisplay container uses circleSize * 0.6f, so we need to work within that
        val containerSize = circleSize * 0.6f
        val availableWidth = containerSize * 0.95f // Use 95% of container width for better space utilization
        val spacing = 3.dp // Reduced spacing to allow for larger dice
        val diceCount = diceResult.values.size

        val diceSize = if (diceCount <= 5) {
            // Single row - calculate size based on available width
            val totalSpacing = spacing * (diceCount - 1)
            val availableForDice = availableWidth - totalSpacing
            val calculatedSize = (availableForDice.value / diceCount).dp

            // Use more generous size limits for better space utilization
            when (diceCount) {
                1 -> calculatedSize.coerceAtMost(120.dp).coerceAtLeast(60.dp)
                2 -> calculatedSize.coerceAtMost(100.dp).coerceAtLeast(50.dp)
                3 -> calculatedSize.coerceAtMost(80.dp).coerceAtLeast(40.dp)
                4 -> calculatedSize.coerceAtMost(70.dp).coerceAtLeast(35.dp)
                5 -> calculatedSize.coerceAtMost(60.dp).coerceAtLeast(30.dp)
                else -> calculatedSize.coerceAtMost(50.dp).coerceAtLeast(25.dp)
            }
        } else {
            // Two rows - calculate based on the larger row
            val firstRowCount = ceil(diceCount / 2.0).toInt()
            val totalSpacing = spacing * (firstRowCount - 1)
            val availableForDice = availableWidth - totalSpacing
            (availableForDice.value / firstRowCount).dp.coerceAtMost(50.dp).coerceAtLeast(20.dp)
        }

        if (diceResult.values.size <= 5) {
            // Single row for 5 or fewer dice
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                diceResult.values.forEach { value ->
                    IndividualDie(
                        diceType = diceType,
                        value = value,
                        size = diceSize,
                        isAnimating = isAnimating
                    )
                }
            }
        } else {
            // Two rows for more than 5 dice
            val firstRowCount = ceil(diceResult.values.size / 2.0).toInt()

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                diceResult.values.take(firstRowCount).forEach { value ->
                    IndividualDie(
                        diceType = diceType,
                        value = value,
                        size = diceSize,
                        isAnimating = isAnimating
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                diceResult.values.drop(firstRowCount).forEach { value ->
                    IndividualDie(
                        diceType = diceType,
                        value = value,
                        size = diceSize,
                        isAnimating = isAnimating
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show total sum
        Text(
            text = "Total: ${diceResult.total}",
            fontSize = (circleSize.value / 12).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun IndividualDie(
    diceType: DiceType,
    value: Int,
    size: Dp,
    isAnimating: Boolean
) {
    // Draw the polygon shape for the die with the number inside
    DicePolygon(
        diceType = diceType,
        size = size,
        isAnimating = isAnimating,
        value = value
    )
}

@Composable
private fun WaitingToThrowDisplay(
    diceType: DiceType,
    diceCount: Int,
    circleSize: Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${diceCount}${diceType.displayName}",
            fontSize = (circleSize.value / 10).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tap to throw",
            fontSize = (circleSize.value / 14).sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show a preview of the die shape
        DicePolygon(
            diceType = diceType,
            size = circleSize * 0.25f,
            isAnimating = false
        )
    }
}

@Composable
private fun DicePolygon(
    diceType: DiceType,
    size: Dp,
    isAnimating: Boolean,
    value: Int? = null // Optional value for dice display
) {
    // Synchronize rotation changes with value changes
    val animationRotation = if (isAnimating && value != null) {
        // Create a rotation that changes when the value changes
        var currentRotation by remember { mutableStateOf(0f) }
        LaunchedEffect(value) {
            // When value changes, pick a new random rotation with more possibilities
            // Use 72 different orientations (every 5 degrees) to ensure visible changes
            currentRotation = (0..71).random() * 5f
        }
        currentRotation
    } else {
        0f
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Draw the polygon shape
        Canvas(
            modifier = Modifier.size(size)
        ) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = size.toPx() / 2 * 0.8f

            rotate(animationRotation, center) {
                when (diceType) {
                    DiceType.D2 -> drawCircle(center, radius)
                    DiceType.D4 -> drawTriangle(center, radius)
                    DiceType.D6 -> drawSquare(center, radius)
                    DiceType.D8 -> drawOctagon(center, radius)
                    DiceType.D10 -> drawDecagon(center, radius)
                    DiceType.D20 -> drawIcosagon(center, radius)
                    DiceType.D100 -> drawDecagon(center, radius) // Use decagon for d100
                }
            }
        }

        // Draw the value as a Text component overlayed on top
        if (value != null) {
            Text(
                text = value.toString(),
                fontSize = (size.value / 3.5f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}

private fun DrawScope.drawCircle(center: Offset, radius: Float) {
    drawCircle(
        color = Color.Blue,
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawTriangle(center: Offset, radius: Float) {
    val path = Path().apply {
        val angle = 2 * PI / 3
        for (i in 0..2) {
            val x = center.x + radius * cos(angle * i - PI / 2).toFloat()
            val y = center.y + radius * sin(angle * i - PI / 2).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = Color.Green,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawSquare(center: Offset, radius: Float) {
    val side = radius * sqrt(2f)
    drawRect(
        color = Color.Red,
        topLeft = Offset(center.x - side / 2, center.y - side / 2),
        size = Size(side, side),
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawPolygon(center: Offset, radius: Float, sides: Int, color: Color) {
    val path = Path().apply {
        val angle = 2 * PI / sides
        for (i in 0 until sides) {
            val x = center.x + radius * cos(angle * i - PI / 2).toFloat()
            val y = center.y + radius * sin(angle * i - PI / 2).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawOctagon(center: Offset, radius: Float) {
    drawPolygon(center, radius, 8, Color.Magenta)
}

private fun DrawScope.drawDecagon(center: Offset, radius: Float) {
    drawPolygon(center, radius, 10, Color.Cyan)
}

private fun DrawScope.drawIcosagon(center: Offset, radius: Float) {
    drawPolygon(center, radius, 20, Color.Yellow)
}
