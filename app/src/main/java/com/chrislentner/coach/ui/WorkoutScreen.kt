package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel
) {
    val uiState = viewModel.uiState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WorkoutUiState.Loading -> {
                CircularProgressIndicator()
            }
            is WorkoutUiState.Active -> {
                ActiveWorkoutView(
                    state = uiState,
                    onCompleteStep = { viewModel.completeCurrentStep() }
                )
            }
            is WorkoutUiState.FreeEntry -> {
                FreeEntryView(
                    onLogEntry = { name, load, reps -> viewModel.logFreeEntry(name, load, reps) },
                    onDone = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
                )
            }
        }
    }
}

@Composable
fun ActiveWorkoutView(
    state: WorkoutUiState.Active,
    onCompleteStep: () -> Unit
) {
    val step = state.currentStep ?: return // Should not happen in Active state

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step ${state.completedStepsCount + 1} / ${state.totalStepsCount}",
            style = MaterialTheme.typography.labelLarge
        )

        Text(
            text = step.exerciseName,
            style = MaterialTheme.typography.displaySmall
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step.targetReps != null) {
                    Text("Reps: ${step.targetReps}", style = MaterialTheme.typography.headlineMedium)
                }
                if (step.targetDurationSeconds != null) {
                     Text("Duration: ${step.targetDurationSeconds}s", style = MaterialTheme.typography.headlineMedium)
                }
                Text("Load: ${step.loadDescription}", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Huge Accept Button
        Button(
            onClick = onCompleteStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("COMPLETE NEXT STEP", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Small Buttons (Disabled)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {}, enabled = false) { Text("Edit") }
            // Skip should record skipped entry in DB (Future impl)
            Button(onClick = {}, enabled = false) { Text("Skip") }
            Button(onClick = {}, enabled = false) { Text("Swap") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {}, enabled = false) { Text("Undo") }
            // Rest Timer: Visual countdown (Future impl)
            Button(onClick = {}, enabled = false) { Text("Rest Timer") }
        }
    }
}

@Composable
fun FreeEntryView(
    onLogEntry: (String, String, String) -> Unit,
    onDone: () -> Unit
) {
    var exercise by remember { mutableStateOf("") }
    var load by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Workout Plan Complete!", style = MaterialTheme.typography.headlineSmall)
        Text("Free Entry Mode", style = MaterialTheme.typography.labelMedium)

        OutlinedTextField(
            value = exercise,
            onValueChange = { exercise = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = load,
                onValueChange = { load = it },
                label = { Text("Load") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                if (exercise.isNotBlank()) {
                    onLogEntry(exercise, load, reps)
                    // Reset fields
                    exercise = ""
                    load = ""
                    reps = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Set")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done (Finish Workout)")
        }
    }
}
