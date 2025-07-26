package com.example.boardgametimer

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boardgametimer.model.GameConfiguration
import com.example.boardgametimer.ui.components.GameTimerScreen
import com.example.boardgametimer.ui.components.StatsDialog
import com.example.boardgametimer.ui.components.TimeExpiredDialog
import com.example.boardgametimer.ui.theme.BoardGameTimerTheme
import com.example.boardgametimer.viewmodel.GameTimerViewModel
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    private val gson = Gson()
    lateinit var viewModel: GameTimerViewModel

    // Register activity launcher for options
    private val optionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra("gameConfiguration")?.let { configJson ->
                try {
                    val newConfig = gson.fromJson(configJson, GameConfiguration::class.java)
                    viewModel.updateConfiguration(newConfig)
                } catch (e: Exception) {
                    // Handle JSON parsing error gracefully
                }
            }
        }
    }

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
                    BoardGameTimerApp(
                        onShowOptions = { config ->
                            val intent = Intent(this@MainActivity, OptionsActivity::class.java).apply {
                                putExtra("gameConfiguration", gson.toJson(config))
                            }
                            optionsLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BoardGameTimerApp(onShowOptions: (GameConfiguration) -> Unit) {
    val viewModel: GameTimerViewModel = viewModel()
    val context = LocalContext.current

    // Store viewModel reference in MainActivity
    LaunchedEffect(viewModel) {
        if (context is MainActivity) {
            context.viewModel = viewModel
        }
    }

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
            onShowOptions = { onShowOptions(viewModel.gameConfiguration) },
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