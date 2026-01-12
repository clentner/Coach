package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.WorkoutEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.worker.WorkoutReminderWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(
    navController: NavController,
    repository: WorkoutRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var initialHour by remember { mutableIntStateOf(8) }
    var initialMinute by remember { mutableIntStateOf(0) }
    var duration by remember { mutableFloatStateOf(60f) }
    var location by remember { mutableStateOf("Home") }

    LaunchedEffect(Unit) {
        val lastWorkout = repository.getLastWorkout()
        if (lastWorkout != null) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = lastWorkout.timeInMillis
            initialHour = cal.get(Calendar.HOUR_OF_DAY)
            initialMinute = cal.get(Calendar.MINUTE)
            duration = lastWorkout.durationMinutes.toFloat()
            location = lastWorkout.location
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Plan your workout", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            TimeInput(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            Text("Duration: ${duration.toInt()} mins")
            Slider(
                value = duration,
                onValueChange = { duration = it },
                valueRange = 20f..150f,
                steps = ((150 - 20) / 5) - 1
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = location == "Home",
                    onClick = { location = "Home" }
                )
                Text("Home")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = location == "Gym",
                    onClick = { location = "Gym" }
                )
                Text("Gym")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        calendar.set(Calendar.SECOND, 0)

                        val entry = WorkoutEntry(
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                            timeInMillis = calendar.timeInMillis,
                            durationMinutes = duration.toInt(),
                            location = location
                        )
                        repository.saveWorkout(entry)

                        // Schedule Reminder
                        val delay = calendar.timeInMillis - System.currentTimeMillis() - (10 * 60 * 1000)
                        if (delay > 0) {
                             val workManager = WorkManager.getInstance(context)
                             workManager.cancelAllWorkByTag("WorkoutReminder")

                             val reminderRequest = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
                                 .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                 .addTag("WorkoutReminder")
                                 .build()

                             workManager.enqueue(reminderRequest)
                        }

                        navController.navigate("home") {
                            popUpTo("survey") { inclusive = true }
                        }
                    }
                }
            ) {
                Text("Save Workout")
            }
        }
    }
}
