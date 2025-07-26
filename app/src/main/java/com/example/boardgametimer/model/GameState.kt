package com.example.boardgametimer.model

enum class DicePhaseState {
    WAITING_TO_THROW,
    THROWING,
    SHOWING_RESULT
}

data class DiceResult(
    val values: List<Int>,
    val total: Int
)

data class GameState(
    val currentPlayerIndex: Int = 0,
    val currentPhaseIndex: Int = 0, // Track current phase within the turn
    val timeRemainingSeconds: Int = 120,
    val timeRemainingMillis: Long = 120000L, // Add millisecond precision for smooth animation
    val isGameRunning: Boolean = false,
    val isPaused: Boolean = true, // Start paused by default
    val roundCount: Int = 0,
    val playerTurnTimes: List<MutableList<Int>> = emptyList(), // Track turn times for each player
    val gameStartTime: Long = 0L,
    val turnStartTime: Long = 0L,
    val showTimeExpiredDialog: Boolean = false, // Show timeout dialog
    val dicePhaseState: DicePhaseState = DicePhaseState.WAITING_TO_THROW,
    val diceResult: DiceResult? = null,
    val isDiceAnimating: Boolean = false
) {
    fun getAverageTimeForPlayer(playerIndex: Int): Double {
        return if (playerIndex < playerTurnTimes.size && playerTurnTimes[playerIndex].isNotEmpty()) {
            playerTurnTimes[playerIndex].average()
        } else {
            0.0
        }
    }

    fun getTotalTimeForPlayer(playerIndex: Int): Int {
        return if (playerIndex < playerTurnTimes.size) {
            playerTurnTimes[playerIndex].sum()
        } else {
            0
        }
    }

    fun getOverallAverageTime(): Double {
        val allTimes = playerTurnTimes.flatten()
        return if (allTimes.isNotEmpty()) {
            allTimes.average()
        } else {
            0.0
        }
    }

    fun getTotalRounds(): Int {
        return playerTurnTimes.maxOfOrNull { it.size } ?: 0
    }
}
