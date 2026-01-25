package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.ScheduleRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    navController: NavController,
    repository: ScheduleRepository
) {
    var workoutText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val schedule = repository.getScheduleByDate(today)
        if (schedule != null) {
            if (schedule.isRestDay) {
                workoutText = "Rest Day"
            } else if (schedule.timeInMillis != null) {
                val instant = Instant.ofEpochMilli(schedule.timeInMillis)
                val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                val timeStr = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                workoutText = "Workout scheduled for today:\n\nTime: $timeStr\nLocation: ${schedule.location}\nDuration: ${schedule.durationMinutes} mins"
            } else {
                workoutText = "Workout planned for today."
            }
        } else {
            workoutText = "No workout planned for today yet."
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = workoutText,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Optional: Button to edit/plan if not planned?
            Button(onClick = { navController.navigate("survey") }) {
                Text("Edit Today's Plan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("weekly_planner") }) {
                Text("Weekly Planner")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("suggest_schedule") }) {
                Text("Suggest Schedule")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("workout") }) {
                Text("Start Workout")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("past_workouts") }) {
                Text("Past Workouts")
            }
        }
    }
}
