package com.example.boardgametimer.model

data class TurnPhase(
    val name: String,
    val durationSeconds: Int
)

data class GameConfiguration(
    val numberOfPlayers: Int = 4,
    val turnPhases: List<TurnPhase> = listOf(TurnPhase("Main Turn", 120)), // Default single phase
    val turnDurationSeconds: Int = 120, // Keep for backward compatibility
    val playerNames: List<String> = emptyList(),
    val audioAlert60s: Boolean = false,
    val audioAlert30s: Boolean = false,
    val audioAlert10sCountdown: Boolean = true, // Enable by default
    val audioAlertTimeOut: Boolean = true, // Enable timeout alert by default
    val vibrateOnAlerts: Boolean = true, // Enable by default
    val keepCounterAlignmentFixed: Boolean = false,
    val selectedSoundIndex: Int = 0 // Index for predefined sounds
) {
    fun getPlayerName(index: Int): String {
        return if (index < playerNames.size && playerNames[index].isNotBlank()) {
            playerNames[index]
        } else {
            "Player ${index + 1}"
        }
    }

    fun getCurrentPhaseDuration(phaseIndex: Int): Int {
        return if (phaseIndex < turnPhases.size) {
            turnPhases[phaseIndex].durationSeconds
        } else {
            turnDurationSeconds // Fallback to legacy duration
        }
    }

    fun getCurrentPhaseName(phaseIndex: Int): String {
        return if (phaseIndex < turnPhases.size) {
            turnPhases[phaseIndex].name
        } else {
            "Phase ${phaseIndex + 1}"
        }
    }

    fun getTotalPhases(): Int = turnPhases.size
}

data class SavedConfiguration(
    val name: String,
    val configuration: GameConfiguration
)
