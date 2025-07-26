package com.example.boardgametimer.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.model.GameState
import com.example.boardgametimer.model.SavedConfiguration
import com.example.boardgametimer.model.DicePhaseState
import com.example.boardgametimer.model.PhaseType
import com.example.boardgametimer.model.TurnPhase
import com.example.boardgametimer.model.DiceResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameTimerViewModel : ViewModel() {

    var gameState by mutableStateOf(GameState())
        private set

    var gameConfiguration by mutableStateOf(GameConfiguration())
        private set

    var showOptions by mutableStateOf(false)
        private set

    var showStats by mutableStateOf(false)
        private set

    var undoHistory by mutableStateOf<GameState?>(null)
        private set

    var savedConfigurations by mutableStateOf<List<SavedConfiguration>>(emptyList())
        private set

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()

    fun initializeAudio(context: Context) {
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

        // Use modern vibrator API
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                ttsReady = true
            }
        }

        // Initialize SharedPreferences for saving configurations
        sharedPreferences = context.getSharedPreferences("BoardGameTimer", Context.MODE_PRIVATE)
        loadSavedConfigurations()
    }

    fun startNewGame() {
        val playerTurnTimes = List(gameConfiguration.numberOfPlayers) { mutableListOf<Int>() }
        val firstPhaseDuration = gameConfiguration.getCurrentPhaseDuration(0)
        gameState = GameState(
            currentPlayerIndex = 0,
            currentPhaseIndex = 0, // Start with first phase
            timeRemainingSeconds = firstPhaseDuration,
            timeRemainingMillis = firstPhaseDuration * 1000L,
            isGameRunning = true,
            isPaused = false, // Start ready to play, not paused
            roundCount = 0,
            playerTurnTimes = playerTurnTimes,
            gameStartTime = System.currentTimeMillis(),
            turnStartTime = System.currentTimeMillis()
        )
        startTimer() // Auto-start the timer for the first player
    }

    fun nextPlayer() {
        if (!gameState.isGameRunning) return

        // Check if we need to advance to next phase or next player
        val nextPhaseIndex = gameState.currentPhaseIndex + 1
        val totalPhases = gameConfiguration.getTotalPhases()

        if (nextPhaseIndex < totalPhases) {
            // Move to next phase for same player
            nextPhase()
        } else {
            // Move to next player (reset to first phase)
            moveToNextPlayer()
        }
    }

    private fun nextPhase() {
        // Play a beep sound when passing to next phase
        playTurnPassBeep()

        // Save current state for undo
        undoHistory = gameState.copy()

        val nextPhaseIndex = gameState.currentPhaseIndex + 1
        val phaseDuration = gameConfiguration.getCurrentPhaseDuration(nextPhaseIndex)
        val nextPhase = gameConfiguration.turnPhases[nextPhaseIndex]

        gameState = gameState.copy(
            currentPhaseIndex = nextPhaseIndex,
            timeRemainingSeconds = phaseDuration,
            timeRemainingMillis = phaseDuration * 1000L,
            turnStartTime = System.currentTimeMillis(),
            isPaused = false,
            // Reset dice state for new phase
            dicePhaseState = DicePhaseState.WAITING_TO_THROW,
            diceResult = null,
            isDiceAnimating = false
        )

        // Auto-start timer for next phase (unless it's a dice phase waiting for throw)
        if (nextPhase.phaseType != PhaseType.DICE_THROW || !shouldWaitForDiceThrow()) {
            startTimer()
        }
    }

    private fun moveToNextPlayer() {
        // Play a beep sound when passing turn
        playTurnPassBeep()

        // Save current state for undo
        undoHistory = gameState.copy()

        // Record the time taken for current player's turn (only record when completing all phases)
        val timeTaken = gameConfiguration.getCurrentPhaseDuration(gameState.currentPhaseIndex) - gameState.timeRemainingSeconds
        val currentPlayerTimes = gameState.playerTurnTimes[gameState.currentPlayerIndex].toMutableList()
        currentPlayerTimes.add(timeTaken)

        val updatedPlayerTurnTimes = gameState.playerTurnTimes.toMutableList()
        updatedPlayerTurnTimes[gameState.currentPlayerIndex] = currentPlayerTimes

        val nextPlayerIndex = (gameState.currentPlayerIndex + 1) % gameConfiguration.numberOfPlayers
        val newRoundCount = if (nextPlayerIndex == 0) gameState.roundCount + 1 else gameState.roundCount
        val firstPhaseDuration = gameConfiguration.getCurrentPhaseDuration(0)
        val firstPhase = gameConfiguration.turnPhases[0]

        gameState = gameState.copy(
            currentPlayerIndex = nextPlayerIndex,
            currentPhaseIndex = 0, // Reset to first phase for new player
            timeRemainingSeconds = firstPhaseDuration,
            timeRemainingMillis = firstPhaseDuration * 1000L,
            playerTurnTimes = updatedPlayerTurnTimes,
            roundCount = newRoundCount,
            turnStartTime = System.currentTimeMillis(),
            isPaused = false,
            // Reset dice state for new player
            dicePhaseState = DicePhaseState.WAITING_TO_THROW,
            diceResult = null,
            isDiceAnimating = false
        )

        // Auto-start timer for next player (unless first phase is a dice phase waiting for throw)
        if (firstPhase.phaseType != PhaseType.DICE_THROW || !shouldWaitForDiceThrow()) {
            startTimer()
        }
    }

    fun undoLastAction() {
        undoHistory?.let { previousState ->
            gameState = previousState
            undoHistory = null
            if (gameState.isGameRunning && !gameState.isPaused) {
                startTimer()
            }
        }
    }

    fun pauseResumeGame() {
        if (gameState.isGameRunning) {
            gameState = gameState.copy(isPaused = !gameState.isPaused)
            if (gameState.isPaused) {
                timerJob?.cancel()
            } else {
                startTimer()
            }
        }
    }

    fun endGame() {
        timerJob?.cancel()
        gameState = gameState.copy(isGameRunning = false, isPaused = false)
        showStats = true
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastUpdateTime = System.currentTimeMillis()
            var lastSecond = gameState.timeRemainingSeconds

            while (gameState.timeRemainingMillis > 0 && gameState.isGameRunning && !gameState.isPaused) {
                delay(16) // Update every 16ms for smooth animation (~60fps)

                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastUpdateTime
                lastUpdateTime = currentTime

                val newTimeMillis = (gameState.timeRemainingMillis - deltaTime).coerceAtLeast(0L)
                val newTimeSeconds = (newTimeMillis / 1000).toInt()

                gameState = gameState.copy(
                    timeRemainingMillis = newTimeMillis,
                    timeRemainingSeconds = newTimeSeconds
                )

                // Handle audio alerts (only on second changes)
                if (newTimeSeconds != lastSecond) {
                    lastSecond = newTimeSeconds
                    when (newTimeSeconds) {
                        60 -> if (gameConfiguration.audioAlert60s) playAlert()
                        30 -> if (gameConfiguration.audioAlert30s) playAlert()
                        10, 9, 8, 7, 6, 5, 4, 3, 2, 1 -> {
                            if (gameConfiguration.audioAlert10sCountdown) {
                                speakCountdown(newTimeSeconds)
                            }
                        }
                        0 -> {
                            // Handle timeout with enhanced functionality
                            handlePlayerTimeout()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun handlePlayerTimeout() {
        // Play timeout alert if enabled
        if (gameConfiguration.audioAlertTimeOut) {
            playAlert()
        }

        // Announce timeout with text-to-speech if enabled
        if (gameConfiguration.audioAlertTimeOut && ttsReady) {
            val playerName = gameConfiguration.getPlayerName(gameState.currentPlayerIndex)
            textToSpeech?.speak("$playerName ran out of time!", TextToSpeech.QUEUE_FLUSH, null, null)
        }

        // Pause the game and show timeout dialog
        gameState = gameState.copy(
            isPaused = true,
            showTimeExpiredDialog = true
        )
    }

    fun dismissTimeExpiredDialog() {
        gameState = gameState.copy(showTimeExpiredDialog = false)
    }

    fun continueAfterTimeout() {
        // Reset timer to full duration and resume game
        gameState = gameState.copy(
            timeRemainingSeconds = gameConfiguration.turnDurationSeconds,
            timeRemainingMillis = gameConfiguration.turnDurationSeconds * 1000L,
            isPaused = false,
            showTimeExpiredDialog = false
        )
        startTimer()
    }

    private fun playAlert() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        if (gameConfiguration.vibrateOnAlerts) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun playTurnPassBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        if (gameConfiguration.vibrateOnAlerts) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun speakCountdown(seconds: Int) {
        if (ttsReady) {
            textToSpeech?.speak(seconds.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
        if (gameConfiguration.vibrateOnAlerts) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun updateConfiguration(newConfig: GameConfiguration) {
        gameConfiguration = newConfig
        // Reset game state with new configuration only if game is not running
        if (!gameState.isGameRunning) {
            val playerTurnTimes = List(newConfig.numberOfPlayers) { mutableListOf<Int>() }
            gameState = GameState(
                timeRemainingSeconds = newConfig.turnDurationSeconds,
                timeRemainingMillis = newConfig.turnDurationSeconds * 1000L,
                playerTurnTimes = playerTurnTimes,
                isPaused = false, // Ready to start, not paused
                isGameRunning = false // Not running until user starts
            )
        }
    }

    fun saveConfiguration(name: String) {
        if (name.isBlank()) return

        val newConfig = SavedConfiguration(name, gameConfiguration)
        val updatedConfigs = savedConfigurations.toMutableList()

        // Remove existing config with same name
        updatedConfigs.removeAll { it.name == name }
        updatedConfigs.add(newConfig)

        savedConfigurations = updatedConfigs
        saveToPersistentStorage()
    }

    fun loadConfiguration(savedConfig: SavedConfiguration) {
        gameConfiguration = savedConfig.configuration
        // Reset game state with new configuration only if game is not running
        if (!gameState.isGameRunning) {
            val playerTurnTimes = List(gameConfiguration.numberOfPlayers) { mutableListOf<Int>() }
            gameState = GameState(
                timeRemainingSeconds = gameConfiguration.turnDurationSeconds,
                timeRemainingMillis = gameConfiguration.turnDurationSeconds * 1000L,
                playerTurnTimes = playerTurnTimes,
                isPaused = false, // Ready to start, not paused
                isGameRunning = false // Not running until user starts
            )
        }
    }

    fun deleteConfiguration(configName: String) {
        savedConfigurations = savedConfigurations.filter { it.name != configName }
        saveToPersistentStorage()
    }

    private fun loadSavedConfigurations() {
        sharedPreferences?.let { prefs ->
            val json = prefs.getString("saved_configurations", "[]")
            val type = object : TypeToken<List<SavedConfiguration>>() {}.type
            try {
                savedConfigurations = gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                savedConfigurations = emptyList()
            }
        }
    }

    private fun saveToPersistentStorage() {
        sharedPreferences?.let { prefs ->
            val json = gson.toJson(savedConfigurations)
            prefs.edit().putString("saved_configurations", json).apply()
        }
    }

    fun showOptions() {
        showOptions = true
    }

    fun hideOptions() {
        showOptions = false
    }

    fun hideStats() {
        showStats = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        toneGenerator?.release()
        textToSpeech?.shutdown()
    }

    fun throwDice() {
        val currentPhase = gameConfiguration.turnPhases[gameState.currentPhaseIndex]
        if (currentPhase.phaseType != PhaseType.DICE_THROW) return

        // Prevent throwing dice if game is not running
        if (!gameState.isGameRunning) return

        // Prevent throwing dice if already thrown in this phase
        if (gameState.dicePhaseState != DicePhaseState.WAITING_TO_THROW) return

        // Start dice animation
        gameState = gameState.copy(
            isDiceAnimating = true,
            dicePhaseState = DicePhaseState.THROWING
        )

        // Simulate dice animation with out-of-sync changes for multiple dice
        viewModelScope.launch {
            val animationDuration = 3000L // Fixed at 3 seconds
            val startTime = System.currentTimeMillis()

            // Initialize with random values immediately to replace "Tap to throw"
            var previousValues = (1..currentPhase.diceCount).map { (1..currentPhase.diceType.sides).random() }
            val lastChangeTime = mutableListOf<Long>()
            repeat(currentPhase.diceCount) { lastChangeTime.add(0L) }

            // Set initial values immediately
            gameState = gameState.copy(
                diceResult = DiceResult(previousValues, previousValues.sum())
            )

            while (System.currentTimeMillis() - startTime < animationDuration) {
                val currentTime = System.currentTimeMillis() - startTime
                val progress = currentTime.toFloat() / animationDuration.toFloat()

                // Create deceleration curve - starts fast, gets slower
                val decelerationFactor = 1.0f - (progress * progress) // Quadratic deceleration
                val adjustedBaseInterval = (120L + (300L * progress)).toLong() // 120ms to 420ms (more controlled)

                // Check each die individually and update immediately when it's time to change
                for (index in 0 until currentPhase.diceCount) {
                    val dieInterval = (adjustedBaseInterval + (index * 80L * decelerationFactor)).toLong()

                    // Check if enough time has passed since this die's last change
                    if (currentTime - lastChangeTime[index] >= dieInterval) {
                        // Time to change this die's value - update immediately
                        lastChangeTime[index] = currentTime
                        val newValue = (1..currentPhase.diceType.sides).random()

                        // Update only this die's value immediately for perfect sync
                        val updatedValues = previousValues.toMutableList()
                        updatedValues[index] = newValue
                        previousValues = updatedValues

                        // Update state immediately when value changes
                        gameState = gameState.copy(
                            diceResult = DiceResult(previousValues, previousValues.sum())
                        )
                    }
                }

                // Fixed frame rate
                delay(50L)
            }

            // Generate final result
            val finalValues = (1..currentPhase.diceCount).map {
                (1..currentPhase.diceType.sides).random()
            }
            gameState = gameState.copy(
                isDiceAnimating = false,
                diceResult = DiceResult(finalValues, finalValues.sum()),
                dicePhaseState = DicePhaseState.SHOWING_RESULT
            )

            // Play dice sound effect
            playDiceSound()

            // Handle timer based on phase configuration
            if (currentPhase.waitForPress) {
                // If wait for press is enabled, just pause and wait for manual progression
                gameState = gameState.copy(isPaused = true)
            } else if (currentPhase.durationSeconds > 0) {
                // Start the timer with the configured duration
                gameState = gameState.copy(
                    timeRemainingSeconds = currentPhase.durationSeconds,
                    timeRemainingMillis = currentPhase.durationSeconds * 1000L,
                    isPaused = false
                )
                startTimer()
            } else {
                // Duration is 0, automatically proceed to next phase/player
                delay(1000) // Show result for 1 second
                nextPlayer()
            }
        }
    }

    private fun playDiceSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    fun getCurrentPhase(): TurnPhase {
        return gameConfiguration.turnPhases[gameState.currentPhaseIndex]
    }

    fun isDicePhase(): Boolean {
        return getCurrentPhase().phaseType == PhaseType.DICE_THROW
    }

    fun shouldWaitForDiceThrow(): Boolean {
        val currentPhase = getCurrentPhase()
        return currentPhase.phaseType == PhaseType.DICE_THROW &&
               gameState.dicePhaseState == DicePhaseState.WAITING_TO_THROW
    }
}
