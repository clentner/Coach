package com.chrislentner.coach

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrislentner.coach.database.AppDatabase
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.ConfigLoader
import com.chrislentner.coach.planner.HistoryAnalyzer
import com.chrislentner.coach.planner.ProgressionEngine
import com.chrislentner.coach.ui.CoachApp
import com.chrislentner.coach.ui.MainViewModel
import com.chrislentner.coach.ui.MainViewModelFactory
import com.chrislentner.coach.ui.theme.CoachTheme
import com.chrislentner.coach.worker.BootReceiver

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { ScheduleRepository(database.scheduleDao()) }
    private val workoutRepository by lazy { WorkoutRepository(database.workoutDao()) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Handle permission granted/rejected if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule the daily survey notification (idempotent)
        BootReceiver.scheduleDailySurvey(applicationContext)

        // Ask for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize Planner
        var planner: AdvancedWorkoutPlanner? = null
        var configExercises: List<String> = emptyList()
        try {
            val config = ConfigLoader.load(applicationContext)
            configExercises = config.getAllExerciseNames()
            val historyAnalyzer = HistoryAnalyzer(config)
            val progressionEngine = ProgressionEngine(historyAnalyzer)
            planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ideally notify user, but for now just logging and fallback to null planner
        }

        val navigateTo = intent.getStringExtra("navigate_to")

        setContent {
            CoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository, navigateTo)
                    )
                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        CoachApp(
                            repository = repository,
                            workoutRepository = workoutRepository,
                            planner = planner,
                            configExercises = configExercises,
                            startDestination = uiState.startDestination
                        )
                    }
                }
            }
        }
    }
}
