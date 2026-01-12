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
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.ui.CoachApp
import com.chrislentner.coach.ui.theme.CoachTheme
import com.chrislentner.coach.worker.BootReceiver
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val repository by lazy { WorkoutRepository(applicationContext) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
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

        // Determine start destination
        var startDestination = "home"

        // Check if opened from notification
        if (intent.getStringExtra("navigate_to") == "survey") {
            startDestination = "survey"
        } else {
            // Check if we have a workout for today
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            // We use runBlocking here for simplicity in onCreate to determine initial state.
            // In a larger app, we'd use a ViewModel and expose state, showing a Splash/Loading first.
            val todaysWorkout = runBlocking { repository.getWorkoutByDate(today) }
            if (todaysWorkout == null) {
                startDestination = "survey"
            }
        }

        setContent {
            CoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoachApp(repository = repository, startDestination = startDestination)
                }
            }
        }
    }
}
