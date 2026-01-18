package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.WorkoutLogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    navController: NavController,
    viewModel: WorkoutDetailViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("historical_workout?sessionId=${viewModel.sessionId}") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exercise", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("Load", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Actual", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }

            items(viewModel.logs) { log ->
                LogEntryRow(log = log)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun LogEntryRow(log: WorkoutLogEntry) {
    val contentColor = if (log.skipped) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(log.exerciseName, modifier = Modifier.weight(2f))
            Text(log.loadDescription, modifier = Modifier.weight(1f))

            val actualText = if (log.actualReps != null) {
                "${log.actualReps} reps"
            } else if (log.actualDurationSeconds != null) {
                "${log.actualDurationSeconds}s"
            } else {
                "-"
            }
            Text(actualText, modifier = Modifier.weight(1f))
        }
    }
}
