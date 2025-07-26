package com.example.boardgametimer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boardgametimer.ui.components.GameTimerScreen
import com.example.boardgametimer.ui.components.OptionsDialog
import com.example.boardgametimer.ui.components.StatsDialog
import com.example.boardgametimer.ui.components.TimeExpiredDialog
import com.example.boardgametimer.ui.theme.BoardGameTimerTheme
import com.example.boardgametimer.viewmodel.GameTimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        setContent {
            BoardGameTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoardGameTimerApp()
                }
            }
        }
    }
}

@Composable
fun BoardGameTimerApp() {
    val viewModel: GameTimerViewModel = viewModel()
    val context = LocalContext.current

    // Initialize audio when the app starts
    LaunchedEffect(Unit) {
        viewModel.initializeAudio(context)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        GameTimerScreen(
            gameState = viewModel.gameState,
            gameConfiguration = viewModel.gameConfiguration,
            onNextPlayer = viewModel::nextPlayer,
            onPauseResume = viewModel::pauseResumeGame,
            onShowOptions = viewModel::showOptions,
            onEndGame = {
                if (viewModel.gameState.isGameRunning) {
                    viewModel.endGame()
                } else {
                    viewModel.startNewGame()
                }
            },
            onUndo = viewModel::undoLastAction,
            modifier = Modifier.padding(innerPadding)
        )

        // Options Dialog
        if (viewModel.showOptions) {
            OptionsDialog(
                gameConfiguration = viewModel.gameConfiguration,
                savedConfigurations = viewModel.savedConfigurations,
                onConfigurationChanged = viewModel::updateConfiguration,
                onSaveConfiguration = { name, config ->
                    // Update the game configuration first, then save it
                    viewModel.updateConfiguration(config)
                    viewModel.saveConfiguration(name)
                },
                onLoadConfiguration = viewModel::loadConfiguration,
                onDeleteConfiguration = viewModel::deleteConfiguration,
                onDismiss = viewModel::hideOptions
            )
        }

        // Stats Dialog
        if (viewModel.showStats) {
            StatsDialog(
                gameState = viewModel.gameState,
                gameConfiguration = viewModel.gameConfiguration,
                onDismiss = viewModel::hideStats,
                onNewGame = viewModel::startNewGame
            )
        }

        // Time Expired Dialog
        if (viewModel.gameState.showTimeExpiredDialog) {
            TimeExpiredDialog(
                currentPlayerIndex = viewModel.gameState.currentPlayerIndex,
                gameConfiguration = viewModel.gameConfiguration,
                onDismiss = { viewModel.dismissTimeExpiredDialog() },
                onNextPlayer = {
                    viewModel.nextPlayer()
                    viewModel.dismissTimeExpiredDialog()
                }
            )
        }
    }
}