package com.example.boardgametimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.SavedConfiguration
import com.example.boardgametimer.model.TurnPhase
import com.example.boardgametimer.ui.theme.BoardGameTimerTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OptionsActivity : ComponentActivity() {
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the current configuration from the intent
        val configJson = intent.getStringExtra("gameConfiguration")
        val currentConfig = if (configJson != null) {
            gson.fromJson(configJson, GameConfiguration::class.java)
        } else {
            GameConfiguration()
        }

        enableEdgeToEdge()
        setContent {
            BoardGameTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptionsScreen(
                        initialConfiguration = currentConfig,
                        onSave = { config ->
                            val resultIntent = Intent().apply {
                                putExtra("gameConfiguration", gson.toJson(config))
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    initialConfiguration: GameConfiguration,
    onSave: (GameConfiguration) -> Unit,
    onCancel: () -> Unit
) {
    var tempConfig by remember { mutableStateOf(initialConfiguration) }
    var playerNames by remember {
        mutableStateOf(
            List(tempConfig.numberOfPlayers) { index ->
                if (index < tempConfig.playerNames.size) tempConfig.playerNames[index] else ""
            }
        )
    }
    var saveConfigName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Load saved configurations
    var savedConfigurations by remember { mutableStateOf<List<SavedConfiguration>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Options") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalConfig = tempConfig.copy(playerNames = playerNames.filter { it.isNotBlank() })
                            onSave(finalConfig)
                        }
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add some top spacing
            item { Spacer(modifier = Modifier.height(8.dp)) }

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
                                fontSize = 18.sp
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
                                            text = "${savedConfig.configuration.numberOfPlayers} players, ${savedConfig.configuration.turnPhases.size} phases",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row {
                                        TextButton(
                                            onClick = {
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
                                            onClick = { /* TODO: Implement delete */ }
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

            // Number of Players
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Number of Players",
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (tempConfig.numberOfPlayers > 1) { // Changed from 2 to 1
                                        tempConfig = tempConfig.copy(numberOfPlayers = tempConfig.numberOfPlayers - 1)
                                        playerNames = playerNames.dropLast(1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease players")
                            }

                            Text(
                                text = tempConfig.numberOfPlayers.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 32.dp)
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

            // Round Configuration
            item {
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
                                fontSize = 18.sp
                            )

                            IconButton(
                                onClick = {
                                    val newPhases = tempConfig.turnPhases + TurnPhase("Phase ${tempConfig.turnPhases.size + 1}", 60)
                                    tempConfig = tempConfig.copy(turnPhases = newPhases)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Phase")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Phase ${index + 1}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
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

                                    Spacer(modifier = Modifier.height(8.dp))

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

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Predefined time options for phase
                                    Text(
                                        text = "Quick Select:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

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

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Phase duration controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Duration:",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
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
                                                fontSize = 18.sp,
                                                modifier = Modifier.widthIn(min = 80.dp)
                                            )

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

            // Player Names
            item {
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
                                fontSize = 18.sp
                            )
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
                        Spacer(modifier = Modifier.height(12.dp))

                        repeat(tempConfig.numberOfPlayers) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
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
                                        .padding(horizontal = 8.dp),
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

            // Audio Alerts
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audio Alerts",
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OptionsCheckboxOption(
                            text = "60 seconds remaining",
                            checked = tempConfig.audioAlert60s,
                            onCheckedChange = { tempConfig = tempConfig.copy(audioAlert60s = it) }
                        )

                        OptionsCheckboxOption(
                            text = "30 seconds remaining",
                            checked = tempConfig.audioAlert30s,
                            onCheckedChange = { tempConfig = tempConfig.copy(audioAlert30s = it) }
                        )

                        OptionsCheckboxOption(
                            text = "10 second countdown",
                            checked = tempConfig.audioAlert10sCountdown,
                            onCheckedChange = { tempConfig = tempConfig.copy(audioAlert10sCountdown = it) }
                        )

                        OptionsCheckboxOption(
                            text = "Time out alert",
                            checked = tempConfig.audioAlertTimeOut,
                            onCheckedChange = { tempConfig = tempConfig.copy(audioAlertTimeOut = it) }
                        )
                    }
                }
            }

            // Other Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Other Options",
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OptionsCheckboxOption(
                            text = "Vibrate on alerts",
                            checked = tempConfig.vibrateOnAlerts,
                            onCheckedChange = { tempConfig = tempConfig.copy(vibrateOnAlerts = it) }
                        )

                        OptionsCheckboxOption(
                            text = "Keep counter alignment fixed",
                            checked = tempConfig.keepCounterAlignmentFixed,
                            onCheckedChange = { tempConfig = tempConfig.copy(keepCounterAlignmentFixed = it) }
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OptionsCheckboxOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}
