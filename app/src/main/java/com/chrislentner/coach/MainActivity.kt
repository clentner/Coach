package com.chrislentner.coach

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.chrislentner.coach.database.AppDatabase
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.ConfigLoader
import com.chrislentner.coach.planner.HistoryAnalyzer
import com.chrislentner.coach.planner.ProgressionEngine
import com.chrislentner.coach.ui.CoachApp
import com.chrislentner.coach.ui.theme.CoachTheme
import com.chrislentner.coach.worker.BootReceiver
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val scheduleRepo by lazy { ScheduleRepository(database.scheduleDao()) }
    private val workoutRepo by lazy { WorkoutRepository(database.workoutDao()) }

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
        try {
            val config = ConfigLoader.load(applicationContext)
            val historyAnalyzer = HistoryAnalyzer(config)
            val progressionEngine = ProgressionEngine(historyAnalyzer)
            planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ideally notify user, but for now just logging and fallback to null planner
        }

        setContent {
            CoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoachApp(
                        workoutRepo = workoutRepo,
                        scheduleRepo = scheduleRepo,
                        planner = planner
                    )
                }
            }
        }
    }
}
