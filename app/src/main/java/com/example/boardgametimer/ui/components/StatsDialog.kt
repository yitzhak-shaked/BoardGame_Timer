package com.example.boardgametimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.GameState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDialog(
    gameState: GameState,
    gameConfiguration: GameConfiguration,
    onDismiss: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // Increased from default to make wider
                .fillMaxHeight(0.85f) // Increased from 0.8f
                .padding(8.dp), // Reduced padding to use more screen space
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp) // Increased from 16.dp
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Statistics",
                        fontSize = 24.sp, // Increased from 20.sp
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // Increased from 16.dp

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp) // Increased from 16.dp
                ) {
                    item {
                        // Overall Stats
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) { // Increased from 16.dp
                                Text(
                                    text = "Overall Statistics",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp // Increased from 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp)) // Increased from 8.dp

                                StatRow("Total Rounds", gameState.getTotalRounds().toString())
                                StatRow("Average Turn Time", formatTime(gameState.getOverallAverageTime().toInt()))
                                StatRow("Total Players", gameConfiguration.numberOfPlayers.toString())
                            }
                        }
                    }

                    item {
                        // Player Statistics
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) { // Increased from 16.dp
                                Text(
                                    text = "Player Statistics",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp // Increased from 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp)) // Increased from 8.dp

                                repeat(gameConfiguration.numberOfPlayers) { playerIndex ->
                                    PlayerStatCard(
                                        playerName = gameConfiguration.getPlayerName(playerIndex),
                                        averageTime = gameState.getAverageTimeForPlayer(playerIndex),
                                        totalTurns = if (playerIndex < gameState.playerTurnTimes.size)
                                            gameState.playerTurnTimes[playerIndex].size else 0
                                    )
                                    if (playerIndex < gameConfiguration.numberOfPlayers - 1) {
                                        Spacer(modifier = Modifier.height(12.dp)) // Increased from 8.dp
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // Increased from 16.dp

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased from 8.dp
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", fontSize = 16.sp) // Added larger font size
                    }

                    Button(
                        onClick = {
                            onNewGame()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Game", fontSize = 16.sp) // Added larger font size
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp // Increased from 14.sp
        )
        Text(
            text = value,
            fontSize = 16.sp, // Increased from 14.sp
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlayerStatCard(
    playerName: String,
    averageTime: Double,
    totalTurns: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp) // Increased from 12.dp
        ) {
            Text(
                text = playerName,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp // Increased from 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp)) // Increased from 4.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avg: ${formatTime(averageTime.toInt())}",
                    fontSize = 14.sp, // Increased from 12.sp
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Turns: $totalTurns",
                    fontSize = 14.sp, // Increased from 12.sp
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}
