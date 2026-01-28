package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.worker.ScheduleReminderWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(
    navController: NavController,
    repository: ScheduleRepository,
    date: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) }
    val targetDate = date ?: LocalDate.now().format(dateFormatter)

    var isLoading by remember { mutableStateOf(true) }
    var initialHour by remember { mutableIntStateOf(8) }
    var initialMinute by remember { mutableIntStateOf(0) }
    var duration by remember { mutableFloatStateOf(60f) }
    var location by remember { mutableStateOf("Home") }

    LaunchedEffect(targetDate) {
        val specificSchedule = repository.getScheduleByDate(targetDate)
        // If we have a specific schedule that is NOT a rest day, use it.
        // Otherwise, fall back to the last schedule (any date) to populate defaults.
        val sourceSchedule = if (specificSchedule != null && !specificSchedule.isRestDay) {
            specificSchedule
        } else {
            repository.getLastSchedule()
        }

        if (sourceSchedule != null) {
            if (sourceSchedule.timeInMillis != null) {
                val time = Instant.ofEpochMilli(sourceSchedule.timeInMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                initialHour = time.hour
                initialMinute = time.minute
            }
            if (sourceSchedule.durationMinutes != null) {
                duration = sourceSchedule.durationMinutes.toFloat()
            }
            if (sourceSchedule.location != null) {
                location = sourceSchedule.location
            }
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
            Text("Plan for $targetDate", style = MaterialTheme.typography.headlineMedium)

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
                        val localDate = LocalDate.parse(targetDate, dateFormatter)
                        val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val scheduleDateTime = ZonedDateTime.of(
                            localDate,
                            localTime,
                            ZoneId.systemDefault()
                        ).withSecond(0).withNano(0)

                        val entry = ScheduleEntry(
                            date = targetDate,
                            timeInMillis = scheduleDateTime.toInstant().toEpochMilli(),
                            durationMinutes = duration.toInt(),
                            location = location,
                            isRestDay = false
                        )
                        repository.saveSchedule(entry)

                        // Schedule Reminder
                        val reminderTime = scheduleDateTime.minusMinutes(10)
                        val delay = Duration.between(Instant.now(), reminderTime.toInstant()).toMillis()
                        if (delay > 0) {
                             val workManager = WorkManager.getInstance(context)
                             // Cancel old work by tag (cleanup)
                             workManager.cancelAllWorkByTag("WorkoutReminder")
                             workManager.cancelAllWorkByTag("ScheduleReminder")

                             val reminderRequest = OneTimeWorkRequestBuilder<ScheduleReminderWorker>()
                                 .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                 .addTag("ScheduleReminder")
                                 .build()

                             workManager.enqueue(reminderRequest)
                        }

                        if (date != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("home") {
                                popUpTo("survey") { inclusive = true }
                            }
                        }
                    }
                }
            ) {
                Text("Save Schedule")
            }
        }
    }
}
