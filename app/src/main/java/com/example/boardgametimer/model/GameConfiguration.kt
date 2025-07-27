package com.example.boardgametimer.model

import java.util.UUID
enum class PhaseType {
    NORMAL,
    DICE_THROW
}

enum class DiceType(val sides: Int, val displayName: String) {
    D2(2, "d2"),
    D4(4, "d4"),
    D6(6, "d6"),
    D8(8, "d8"),
    D10(10, "d10"),
    D20(20, "d20"),
    D100(100, "d100")
}

data class TurnPhase(
    val name: String,
    val durationSeconds: Int,
    val phaseType: PhaseType = PhaseType.NORMAL,
    val diceCount: Int = 1,
    val diceType: DiceType = DiceType.D6,
    val waitForPress: Boolean = false,
    val id: String = UUID.randomUUID().toString()
) {
    // Ensure backward compatibility - if id is empty or null, generate a new one
    fun withValidId(): TurnPhase {
        return if (id.isBlank()) {
            copy(id = UUID.randomUUID().toString())
        } else {
            this
        }
    }
}

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
