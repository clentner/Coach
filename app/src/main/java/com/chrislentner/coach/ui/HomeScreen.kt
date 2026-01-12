package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.ScheduleRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    repository: ScheduleRepository
) {
    var workoutText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val schedule = repository.getScheduleByDate(today)
        if (schedule != null) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = schedule.timeInMillis
            val timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            workoutText = "Workout scheduled for today:\n\nTime: $timeStr\nLocation: ${schedule.location}\nDuration: ${schedule.durationMinutes} mins"
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
                Text("Edit Plan")
            }
        }
    }
}
