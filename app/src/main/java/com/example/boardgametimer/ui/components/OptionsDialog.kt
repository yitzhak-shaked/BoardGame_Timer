package com.example.boardgametimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.SavedConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
    gameConfiguration: GameConfiguration,
    savedConfigurations: List<SavedConfiguration> = emptyList(),
    onConfigurationChanged: (GameConfiguration) -> Unit,
    onSaveConfiguration: (String, GameConfiguration) -> Unit = { _, _ -> },
    onLoadConfiguration: (SavedConfiguration) -> Unit = {},
    onDeleteConfiguration: (String) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempConfig by remember { mutableStateOf(gameConfiguration) }
    var playerNames by remember {
        mutableStateOf(
            List(tempConfig.numberOfPlayers) { index ->
                if (index < tempConfig.playerNames.size) tempConfig.playerNames[index] else ""
            }
        )
    }
    var saveConfigName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // Make it 95% of screen width instead of default
                .fillMaxHeight(0.9f)
                .padding(2.dp), // Reduced padding for more space
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Options",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Saved Configurations
                    if (savedConfigurations.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Saved Configurations",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    savedConfigurations.forEach { savedConfig ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = savedConfig.name,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${savedConfig.configuration.numberOfPlayers} players, ${formatDuration(savedConfig.configuration.turnDurationSeconds)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                            Row {
                                                TextButton(
                                                    onClick = {
                                                        onLoadConfiguration(savedConfig)
                                                        tempConfig = savedConfig.configuration
                                                        playerNames = List(savedConfig.configuration.numberOfPlayers) { index ->
                                                            if (index < savedConfig.configuration.playerNames.size) {
                                                                savedConfig.configuration.playerNames[index]
                                                            } else ""
                                                        }
                                                    }
                                                ) {
                                                    Text("Load")
                                                }
                                                IconButton(
                                                    onClick = { onDeleteConfiguration(savedConfig.name) }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Number of Players
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Number of Players",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (tempConfig.numberOfPlayers > 2) {
                                                tempConfig = tempConfig.copy(numberOfPlayers = tempConfig.numberOfPlayers - 1)
                                                playerNames = playerNames.dropLast(1)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease players")
                                    }

                                    Text(
                                        text = tempConfig.numberOfPlayers.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = {
                                            if (tempConfig.numberOfPlayers < 15) {
                                                tempConfig = tempConfig.copy(numberOfPlayers = tempConfig.numberOfPlayers + 1)
                                                playerNames = playerNames + ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase players")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Turn Phases Configuration (replaces Turn Duration)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Round Configuration",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )

                                    IconButton(
                                        onClick = {
                                            val newPhases = tempConfig.turnPhases + com.example.boardgametimer.model.TurnPhase("Phase ${tempConfig.turnPhases.size + 1}", 60)
                                            tempConfig = tempConfig.copy(turnPhases = newPhases)
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Phase")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Display each phase
                                tempConfig.turnPhases.forEachIndexed { index, phase ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Phase ${index + 1}",
                                                    fontWeight = FontWeight.Medium
                                                )

                                                if (tempConfig.turnPhases.size > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val newPhases = tempConfig.turnPhases.toMutableList()
                                                            newPhases.removeAt(index)
                                                            tempConfig = tempConfig.copy(turnPhases = newPhases)
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Phase")
                                                    }
                                                }
                                            }

                                            // Phase name input
                                            OutlinedTextField(
                                                value = phase.name,
                                                onValueChange = { newName ->
                                                    val newPhases = tempConfig.turnPhases.toMutableList()
                                                    newPhases[index] = phase.copy(name = newName)
                                                    tempConfig = tempConfig.copy(turnPhases = newPhases)
                                                },
                                                label = { Text("Phase Name") },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Predefined time options for phase
                                            Text(
                                                text = "Quick Select:",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))

                                            val timeOptions = listOf(30, 60, 90, 120, 180, 300, 600) // seconds
                                            val timeLabels = listOf("30s", "1m", "1.5m", "2m", "3m", "5m", "10m")

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(timeOptions.size) { timeIndex ->
                                                    FilterChip(
                                                        onClick = {
                                                            val newPhases = tempConfig.turnPhases.toMutableList()
                                                            newPhases[index] = phase.copy(durationSeconds = timeOptions[timeIndex])
                                                            tempConfig = tempConfig.copy(turnPhases = newPhases)
                                                        },
                                                        label = { Text(timeLabels[timeIndex]) },
                                                        selected = phase.durationSeconds == timeOptions[timeIndex]
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Phase duration with original minus button and slightly wider plus button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Duration:",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(0.dp) // No gap
                                                ) {
                                                    // Original minus button (IconButton)
                                                    IconButton(
                                                        onClick = {
                                                            if (phase.durationSeconds > 10) {
                                                                val newPhases = tempConfig.turnPhases.toMutableList()
                                                                newPhases[index] = phase.copy(durationSeconds = phase.durationSeconds - 10)
                                                                tempConfig = tempConfig.copy(turnPhases = newPhases)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease duration")
                                                    }

                                                    Text(
                                                        text = "${phase.durationSeconds / 60}:${String.format("%02d", phase.durationSeconds % 60)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        modifier = Modifier.widthIn(min = 60.dp)
                                                    )

                                                    // Plus button without any extra padding or Box wrapper
                                                    IconButton(
                                                        onClick = {
                                                            if (phase.durationSeconds < 600) {
                                                                val newPhases = tempConfig.turnPhases.toMutableList()
                                                                newPhases[index] = phase.copy(durationSeconds = phase.durationSeconds + 10)
                                                                tempConfig = tempConfig.copy(turnPhases = newPhases)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase duration")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Player Names
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Player Names (Optional)",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    // Flip/Reorder button
                                    IconButton(
                                        onClick = {
                                            playerNames = playerNames.reversed()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Reverse player order"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                repeat(tempConfig.numberOfPlayers) { index ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move up button
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val newNames = playerNames.toMutableList()
                                                    val temp = newNames[index]
                                                    newNames[index] = newNames[index - 1]
                                                    newNames[index - 1] = temp
                                                    playerNames = newNames
                                                }
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move up"
                                            )
                                        }

                                        // Player name text field
                                        OutlinedTextField(
                                            value = if (index < playerNames.size) playerNames[index] else "",
                                            onValueChange = { newName ->
                                                val updatedNames = playerNames.toMutableList()
                                                while (updatedNames.size <= index) {
                                                    updatedNames.add("")
                                                }
                                                updatedNames[index] = newName
                                                playerNames = updatedNames
                                            },
                                            label = { Text("Player ${index + 1}") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp),
                                            singleLine = true
                                        )

                                        // Move down button
                                        IconButton(
                                            onClick = {
                                                if (index < tempConfig.numberOfPlayers - 1) {
                                                    val newNames = playerNames.toMutableList()
                                                    val temp = newNames[index]
                                                    newNames[index] = newNames[index + 1]
                                                    newNames[index + 1] = temp
                                                    playerNames = newNames
                                                }
                                            },
                                            enabled = index < tempConfig.numberOfPlayers - 1
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move down"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Audio Alerts
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Audio Alerts",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                CheckboxOption(
                                    text = "60 seconds remaining",
                                    checked = tempConfig.audioAlert60s,
                                    onCheckedChange = { tempConfig = tempConfig.copy(audioAlert60s = it) }
                                )

                                CheckboxOption(
                                    text = "30 seconds remaining",
                                    checked = tempConfig.audioAlert30s,
                                    onCheckedChange = { tempConfig = tempConfig.copy(audioAlert30s = it) }
                                )

                                CheckboxOption(
                                    text = "10 second countdown",
                                    checked = tempConfig.audioAlert10sCountdown,
                                    onCheckedChange = { tempConfig = tempConfig.copy(audioAlert10sCountdown = it) }
                                )

                                CheckboxOption(
                                    text = "Time out alert",
                                    checked = tempConfig.audioAlertTimeOut,
                                    onCheckedChange = { tempConfig = tempConfig.copy(audioAlertTimeOut = it) }
                                )
                            }
                        }
                    }

                    item {
                        // Other Options
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Other Options",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                CheckboxOption(
                                    text = "Vibrate on alerts",
                                    checked = tempConfig.vibrateOnAlerts,
                                    onCheckedChange = { tempConfig = tempConfig.copy(vibrateOnAlerts = it) }
                                )

                                CheckboxOption(
                                    text = "Keep counter alignment fixed",
                                    checked = tempConfig.keepCounterAlignmentFixed,
                                    onCheckedChange = { tempConfig = tempConfig.copy(keepCounterAlignmentFixed = it) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Configuration Button
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Config")
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            val finalConfig = tempConfig.copy(playerNames = playerNames.filter { it.isNotBlank() })
                            onConfigurationChanged(finalConfig)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    // Save Configuration Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Configuration") },
            text = {
                OutlinedTextField(
                    value = saveConfigName,
                    onValueChange = { saveConfigName = it },
                    label = { Text("Configuration Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveConfigName.isNotBlank()) {
                            val finalConfig = tempConfig.copy(playerNames = playerNames.filter { it.isNotBlank() })
                            onSaveConfiguration(saveConfigName, finalConfig)
                            // Don't apply the configuration when saving - just save it
                            showSaveDialog = false
                            saveConfigName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CheckboxOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${seconds}s"
    }
}

private fun parseDurationInput(input: String): Int {
    // Remove all non-digit characters
    val numericInput = input.filter { it.isDigit() }

    return if (numericInput.isNotEmpty()) {
        // Convert to seconds, assuming the input is in the format of "XmYs" or "Xs"
        var totalSeconds = 0
        var factor = 1
        for (i in numericInput.length - 1 downTo 0) {
            totalSeconds += (numericInput[i].digitToInt() * factor)
            factor *= 10
        }
        totalSeconds
    } else {
        0
    }
}
