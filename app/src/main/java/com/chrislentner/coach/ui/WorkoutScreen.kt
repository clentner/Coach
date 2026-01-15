package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel
) {
    val uiState = viewModel.uiState
    val isMetronomeEnabled = viewModel.isMetronomeEnabled

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
                    isMetronomeEnabled = isMetronomeEnabled,
                    onToggleMetronome = { viewModel.toggleMetronome() },
                    onCompleteStep = { viewModel.completeCurrentStep() },
                    onSkipStep = { viewModel.skipCurrentStep() },
                    onUndo = { viewModel.undoLastStep() },
                    isTimerRunning = viewModel.isTimerRunning,
                    timerStartTime = viewModel.timerStartTime,
                    timerAccumulatedTime = viewModel.timerAccumulatedTime,
                    onTimerToggle = { viewModel.toggleTimer() },
                    onTimerReset = { viewModel.resetTimer() }
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
    isMetronomeEnabled: Boolean,
    onToggleMetronome: () -> Unit,
    onCompleteStep: () -> Unit,
    onSkipStep: () -> Unit,
    onUndo: () -> Unit,
    isTimerRunning: Boolean,
    timerStartTime: Long?,
    timerAccumulatedTime: Long,
    onTimerToggle: () -> Unit,
    onTimerReset: () -> Unit
) {
    val step = state.currentStep ?: return // Should not happen in Active state
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    // Metronome Logic
    LaunchedEffect(step, isMetronomeEnabled, lifecycleState) {
        if (isMetronomeEnabled &&
            lifecycleState == Lifecycle.State.RESUMED &&
            step.tempo != null
        ) {
            val metronome = Metronome()
            try {
                while (isActive) {
                    metronome.playClick()
                    delay(1000)
                }
            } finally {
                metronome.release()
            }
        }
    }

    // Top Right Icon for Metronome
    // We want this floating or in a top bar arrangement.
    // Since ActiveWorkoutView is inside a Box with Center alignment in parent, let's rearrange layout slightly
    // or put the Icon in a Box overlapping this column.

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Content with original spacing logic if possible, or just spaced column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            if (step.tempo != null) {
                                val t = step.tempo
                                val parts = mutableListOf<String>()
                                t.down?.let { parts.add("Down ${it}s") }
                                t.holdBottom?.let { parts.add("Hold ${it}s") }
                                t.up?.let { parts.add("Up ${it}s") }
                                t.holdTop?.let { parts.add("Hold ${it}s") }
                                Text("Tempo: ${parts.joinToString(", ")}", style = MaterialTheme.typography.titleLarge)
                            }
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
                        Button(onClick = onSkipStep) { Text("Skip") }
                        Button(onClick = {}, enabled = false) { Text("Swap") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onUndo,
                            enabled = state.completedStepsCount > 0
                        ) { Text("Undo") }
                    }
                }
            }

            RestTimer(
                isRunning = isTimerRunning,
                startTime = timerStartTime,
                accumulatedTime = timerAccumulatedTime,
                onToggle = onTimerToggle,
                onReset = onTimerReset
            )
        }

        // Volume Toggle Icon
        // Only show if the step is relevant? Or always?
        // Requirement: "functionality should be toggle-able... persisted throughout the workout"
        // It implies the toggle is available generally or at least when metronome is active.
        // Usually such settings are always available or available when applicable.
        // Let's make it always available so user can preemptively turn it off.
        IconButton(
            onClick = onToggleMetronome,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = if (isMetronomeEnabled) "Mute Metronome" else "Unmute Metronome",
                tint = if (isMetronomeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
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
