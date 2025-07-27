package com.example.boardgametimer

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.SavedConfiguration
import com.example.boardgametimer.model.TurnPhase
import com.example.boardgametimer.model.PhaseType
import com.example.boardgametimer.model.DiceType
import com.example.boardgametimer.ui.theme.BoardGameTimerTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import androidx.core.content.edit

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

    // Load saved configurations (memoized to prevent recreation)
    val context = LocalContext.current
    val gson = remember { Gson() }
    val prefs = remember { context.getSharedPreferences("BoardGameTimer", Context.MODE_PRIVATE) }

    var savedConfigurations by remember {
        mutableStateOf<List<SavedConfiguration>>(
            try {
                val json = prefs.getString("saved_configurations", "[]")
                gson.fromJson<List<SavedConfiguration>>(json, object : TypeToken<List<SavedConfiguration>>() {}.type) ?: emptyList<SavedConfiguration>()
            } catch (_: Exception) {
                emptyList<SavedConfiguration>()
            }
        )
    }

    // Memoize callbacks to prevent unnecessary recompositions
    val saveConfigsToPrefs = remember {
        { configs: List<SavedConfiguration> ->
            val json = gson.toJson(configs)
            prefs.edit { putString("saved_configurations", json) }
            savedConfigurations = configs
        }
    }

    val onPlayerCountChange = remember {
        { newCount: Int ->
            tempConfig = tempConfig.copy(numberOfPlayers = newCount)
            playerNames = when {
                newCount > playerNames.size -> playerNames + List(newCount - playerNames.size) { "" }
                newCount < playerNames.size -> playerNames.take(newCount)
                else -> playerNames
            }
        }
    }

    val onPhasesChange = remember {
        { newPhases: List<TurnPhase> ->
            tempConfig = tempConfig.copy(turnPhases = newPhases)
        }
    }

    val onConfigChange = remember {
        { newConfig: GameConfiguration ->
            tempConfig = newConfig
        }
    }

    val onPlayerNamesChange = remember {
        { newNames: List<String> ->
            playerNames = newNames
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Options") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add Save Config button to the top bar
                    TextButton(onClick = { showSaveDialog = true }) {
                        Text("SAVE CONFIG")
                    }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Saved Configurations - Only show if not empty
            if (savedConfigurations.isNotEmpty()) {
                item(
                    key = "saved_configs",
                    contentType = "saved_configs_card"
                ) {
                    SavedConfigurationsCard(
                        savedConfigurations = savedConfigurations,
                        onLoadConfiguration = remember {
                            { savedConfig: SavedConfiguration ->
                                tempConfig = savedConfig.configuration
                                playerNames = List(savedConfig.configuration.numberOfPlayers) { index ->
                                    if (index < savedConfig.configuration.playerNames.size) {
                                        savedConfig.configuration.playerNames[index]
                                    } else ""
                                }
                            }
                        },
                        onDeleteConfiguration = remember {
                            { configName: String ->
                                val updatedConfigs = savedConfigurations.filter { it.name != configName }
                                saveConfigsToPrefs(updatedConfigs)
                            }
                        }
                    )
                }
            }

            // Number of Players
            item(
                key = "player_count",
                contentType = "player_count_card"
            ) {
                PlayerCountCard(
                    numberOfPlayers = tempConfig.numberOfPlayers,
                    onPlayerCountChange = onPlayerCountChange
                )
            }

            // Round Configuration
            item(
                key = "round_config",
                contentType = "round_config_card"
            ) {
                RoundConfigurationCard(
                    turnPhases = tempConfig.turnPhases,
                    onPhasesChange = onPhasesChange
                )
            }

            // Player Names
            item(
                key = "player_names",
                contentType = "player_names_card"
            ) {
                PlayerNamesCard(
                    numberOfPlayers = tempConfig.numberOfPlayers,
                    playerNames = playerNames,
                    onPlayerNamesChange = onPlayerNamesChange
                )
            }

            // Audio Alerts
            item(
                key = "audio_alerts",
                contentType = "audio_alerts_card"
            ) {
                AudioAlertsCard(
                    config = tempConfig,
                    onConfigChange = onConfigChange
                )
            }

            // Other Options
            item(
                key = "other_options",
                contentType = "other_options_card"
            ) {
                OtherOptionsCard(
                    config = tempConfig,
                    onConfigChange = onConfigChange
                )
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
                            val newSavedConfig = SavedConfiguration(saveConfigName, finalConfig)
                            val updatedConfigs = savedConfigurations.toMutableList().apply {
                                removeAll { it.name == saveConfigName }
                                add(newSavedConfig)
                            }
                            saveConfigsToPrefs(updatedConfigs)
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

// Separate composables for each card to improve performance
@Composable
private fun SavedConfigurationsCard(
    savedConfigurations: List<SavedConfiguration>,
    onLoadConfiguration: (SavedConfiguration) -> Unit,
    onDeleteConfiguration: (String) -> Unit
) {
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
                        TextButton(onClick = { onLoadConfiguration(savedConfig) }) {
                            Text("Load")
                        }
                        IconButton(onClick = { onDeleteConfiguration(savedConfig.name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (savedConfig != savedConfigurations.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PlayerCountCard(
    numberOfPlayers: Int,
    onPlayerCountChange: (Int) -> Unit
) {
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
                        if (numberOfPlayers > 1) {
                            onPlayerCountChange(numberOfPlayers - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease players")
                }

                Text(
                    text = numberOfPlayers.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                IconButton(
                    onClick = {
                        if (numberOfPlayers < 15) {
                            onPlayerCountChange(numberOfPlayers + 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase players")
                }
            }
        }
    }
}

@Composable
private fun RoundConfigurationCard(
    turnPhases: List<TurnPhase>,
    onPhasesChange: (List<TurnPhase>) -> Unit
) {
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
                        val newPhases = turnPhases + TurnPhase("Phase ${turnPhases.size + 1}", 60)
                        onPhasesChange(newPhases)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Phase")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Use stable phase ID as key to prevent recomposition issues
            turnPhases.forEachIndexed { index, phase ->
                key(phase.id) {
                    PhaseConfigCard(
                        phase = phase,
                        phaseIndex = index,
                        canDelete = turnPhases.size > 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < turnPhases.size - 1,
                        onPhaseChange = { newPhase ->
                            val newPhases = turnPhases.toMutableList()
                            newPhases[index] = newPhase
                            onPhasesChange(newPhases)
                        },
                        onDeletePhase = {
                            val newPhases = turnPhases.toMutableList()
                            newPhases.removeAt(index)
                            onPhasesChange(newPhases)
                        },
                        onMovePhaseUp = {
                            if (index > 0) {
                                val newPhases = turnPhases.toMutableList()
                                val temp = newPhases[index]
                                newPhases[index] = newPhases[index - 1]
                                newPhases[index - 1] = temp
                                onPhasesChange(newPhases)
                            }
                        },
                        onMovePhaseDown = {
                            if (index < turnPhases.size - 1) {
                                val newPhases = turnPhases.toMutableList()
                                val temp = newPhases[index]
                                newPhases[index] = newPhases[index + 1]
                                newPhases[index + 1] = temp
                                onPhasesChange(newPhases)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseConfigCard(
    phase: TurnPhase,
    phaseIndex: Int,
    canDelete: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPhaseChange: (TurnPhase) -> Unit,
    onDeletePhase: () -> Unit,
    onMovePhaseUp: () -> Unit,
    onMovePhaseDown: () -> Unit
) {
    // Memoize expensive computations
    val timeOptions = remember { listOf(0, 10, 15, 30, 45, 60, 90, 120, 180, 240, 300, 450, 600, 900, 1200) }
    val timeLabels = remember { listOf("Wait", "10s", "15s", "30s", "45s", "1m", "1.5m", "2m", "3m", "4m", "5m", "7.5m", "10m", "15m", "20m") }
    val formattedTime = remember(phase.durationSeconds) {
        if (phase.durationSeconds == 0) "Wait for press"
        else "${phase.durationSeconds / 60}:${String.format(Locale.getDefault(), "%02d", phase.durationSeconds % 60)}"
    }

    // Memoize callbacks with proper dependencies
    val onNameChange = remember(phase) {
        { newName: String ->
            onPhaseChange(phase.copy(name = newName))
        }
    }

    val onDurationDecrease = remember(phase) {
        {
            if (phase.durationSeconds > 0) {
                val newDuration = if (phase.durationSeconds <= 10) 0 else phase.durationSeconds - 10
                onPhaseChange(phase.copy(durationSeconds = newDuration))
            }
        }
    }

    val onDurationIncrease = remember(phase) {
        {
            if (phase.durationSeconds < 600) {
                val newDuration = if (phase.durationSeconds == 0) 10 else phase.durationSeconds + 10
                onPhaseChange(phase.copy(durationSeconds = newDuration))
            }
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Phase ${phaseIndex + 1}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )

                    // Phase reordering buttons
                    Row {
                        IconButton(
                            onClick = onMovePhaseUp,
                            enabled = canMoveUp
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move phase up",
                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        IconButton(
                            onClick = onMovePhaseDown,
                            enabled = canMoveDown
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move phase down",
                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }

                if (canDelete) {
                    IconButton(onClick = onDeletePhase) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Phase")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phase name input
            OutlinedTextField(
                value = phase.name,
                onValueChange = onNameChange,
                label = { Text("Phase Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phase Type Selection
            Text(
                text = "Phase Type:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onPhaseChange(phase.copy(phaseType = PhaseType.NORMAL)) },
                    label = { Text("Normal") },
                    selected = phase.phaseType == PhaseType.NORMAL,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    onClick = { onPhaseChange(phase.copy(phaseType = PhaseType.DICE_THROW)) },
                    label = { Text("Dice Throw") },
                    selected = phase.phaseType == PhaseType.DICE_THROW,
                    modifier = Modifier.weight(1f)
                )
            }

            // Dice Configuration (only show for dice throw phases)
            if (phase.phaseType == PhaseType.DICE_THROW) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Dice Configuration:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dice Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Number of Dice:")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (phase.diceCount > 1) {
                                    onPhaseChange(phase.copy(diceCount = phase.diceCount - 1))
                                }
                            },
                            enabled = phase.diceCount > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease dice count")
                        }

                        Text(
                            text = "${phase.diceCount}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 40.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (phase.diceCount < 10) {
                                    onPhaseChange(phase.copy(diceCount = phase.diceCount + 1))
                                }
                            },
                            enabled = phase.diceCount < 10
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase dice count")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dice Type Selection
                Text(text = "Dice Type:")
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(DiceType.entries.toTypedArray()) { diceType ->
                        FilterChip(
                            onClick = { onPhaseChange(phase.copy(diceType = diceType)) },
                            label = { Text(diceType.displayName) },
                            selected = phase.diceType == diceType
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wait for Press Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Wait for press to throw dice:")
                    Switch(
                        checked = phase.waitForPress,
                        onCheckedChange = { onPhaseChange(phase.copy(waitForPress = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Quick Select:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Replace LazyRow with FlowRow for better performance
            QuickSelectChips(
                timeOptions = timeOptions,
                timeLabels = timeLabels,
                currentDuration = phase.durationSeconds,
                onDurationSelect = { duration ->
                    onPhaseChange(phase.copy(durationSeconds = duration))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration controls
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
                    IconButton(onClick = onDurationDecrease) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease duration")
                    }

                    Text(
                        text = formattedTime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.widthIn(min = 80.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = onDurationIncrease) {
                        Icon(Icons.Default.Add, contentDescription = "Increase duration")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSelectChips(
    timeOptions: List<Int>,
    timeLabels: List<String>,
    currentDuration: Int,
    onDurationSelect: (Int) -> Unit
) {
    // Use horizontally scrollable LazyRow for better UX with many options
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(timeOptions.size) { index ->
            FilterChip(
                onClick = { onDurationSelect(timeOptions[index]) },
                label = { Text(timeLabels[index]) },
                selected = currentDuration == timeOptions[index]
            )
        }
    }
}

@Composable
private fun PlayerNamesCard(
    numberOfPlayers: Int,
    playerNames: List<String>,
    onPlayerNamesChange: (List<String>) -> Unit
) {
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
                        onPlayerNamesChange(playerNames.reversed())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Reverse player order"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            repeat(numberOfPlayers) { index ->
                key(index) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp), // Reduced padding
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
                                    onPlayerNamesChange(newNames)
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
                                onPlayerNamesChange(updatedNames)
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
                                if (index < numberOfPlayers - 1) {
                                    val newNames = playerNames.toMutableList()
                                    val temp = newNames[index]
                                    newNames[index] = newNames[index + 1]
                                    newNames[index + 1] = temp
                                    onPlayerNamesChange(newNames)
                                }
                            },
                            enabled = index < numberOfPlayers - 1
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
}

@Composable
private fun AudioAlertsCard(
    config: GameConfiguration,
    onConfigChange: (GameConfiguration) -> Unit
) {
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
                checked = config.audioAlert60s,
                onCheckedChange = { onConfigChange(config.copy(audioAlert60s = it)) }
            )

            OptionsCheckboxOption(
                text = "30 seconds remaining",
                checked = config.audioAlert30s,
                onCheckedChange = { onConfigChange(config.copy(audioAlert30s = it)) }
            )

            OptionsCheckboxOption(
                text = "10 second countdown",
                checked = config.audioAlert10sCountdown,
                onCheckedChange = { onConfigChange(config.copy(audioAlert10sCountdown = it)) }
            )

            OptionsCheckboxOption(
                text = "Time out alert",
                checked = config.audioAlertTimeOut,
                onCheckedChange = { onConfigChange(config.copy(audioAlertTimeOut = it)) }
            )
        }
    }
}

@Composable
private fun OtherOptionsCard(
    config: GameConfiguration,
    onConfigChange: (GameConfiguration) -> Unit
) {
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
                checked = config.vibrateOnAlerts,
                onCheckedChange = { onConfigChange(config.copy(vibrateOnAlerts = it)) }
            )

            OptionsCheckboxOption(
                text = "Keep counter alignment fixed",
                checked = config.keepCounterAlignmentFixed,
                onCheckedChange = { onConfigChange(config.copy(keepCounterAlignmentFixed = it)) }
            )
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
