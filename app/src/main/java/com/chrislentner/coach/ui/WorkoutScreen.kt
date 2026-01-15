package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
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

    // Free Entry State (Hoisted to share with Metronome logic)
    var freeExercise by remember { mutableStateOf("") }
    var freeLoad by remember { mutableStateOf("") }
    var freeReps by remember { mutableStateOf("") }
    var freeTempo by remember { mutableStateOf("") }

    // Calculate effective tempo for metronome
    val effectiveTempo = when (uiState) {
        is WorkoutUiState.Active -> uiState.currentStep?.tempo
        is WorkoutUiState.FreeEntry -> if (freeTempo.length == 4 && freeTempo.all { it.isDigit() }) freeTempo else null
        else -> null
    }

    // Determine completed/total count for header
    val (stepHeader, isFreeMode) = when (uiState) {
        is WorkoutUiState.Active -> "Step ${uiState.completedStepsCount + 1} / ${uiState.totalStepsCount}" to false
        is WorkoutUiState.FreeEntry -> "Free Entry Mode" to true
        else -> "" to false
    }

    if (uiState is WorkoutUiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        SessionScreenContent(
            isFreeMode = isFreeMode,
            stepHeader = stepHeader,
            // Active Step Data
            activeStepName = (uiState as? WorkoutUiState.Active)?.currentStep?.exerciseName,
            activeStepReps = (uiState as? WorkoutUiState.Active)?.currentStep?.targetReps?.toString(),
            activeStepDuration = (uiState as? WorkoutUiState.Active)?.currentStep?.targetDurationSeconds?.toString(),
            activeStepLoad = (uiState as? WorkoutUiState.Active)?.currentStep?.loadDescription,
            activeStepTempo = (uiState as? WorkoutUiState.Active)?.currentStep?.tempo,
            // Metronome
            isMetronomeEnabled = isMetronomeEnabled,
            effectiveTempo = effectiveTempo,
            onToggleMetronome = { viewModel.toggleMetronome() },
            // Timer
            isTimerRunning = viewModel.isTimerRunning,
            timerStartTime = viewModel.timerStartTime,
            timerAccumulatedTime = viewModel.timerAccumulatedTime,
            onTimerToggle = { viewModel.toggleTimer() },
            onTimerReset = { viewModel.resetTimer() },
            // Actions
            onCompleteStep = { viewModel.completeCurrentStep() },
            onSkipStep = { viewModel.skipCurrentStep() },
            onUndo = { viewModel.undoLastStep() },
            canUndo = (uiState is WorkoutUiState.Active && uiState.completedStepsCount > 0) || (uiState is WorkoutUiState.FreeEntry),
            onFinishWorkout = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
            // Free Entry Data
            freeExercise = freeExercise,
            onFreeExerciseChange = { freeExercise = it },
            freeLoad = freeLoad,
            onFreeLoadChange = { freeLoad = it },
            freeReps = freeReps,
            onFreeRepsChange = { freeReps = it },
            freeTempo = freeTempo,
            onFreeTempoChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) freeTempo = it },
            onLogFreeEntry = {
                if (freeExercise.isNotBlank()) {
                    viewModel.logFreeEntry(
                        freeExercise,
                        freeLoad,
                        freeReps,
                        if (freeTempo.length == 4) freeTempo else null
                    )
                    // Reset fields
                    freeExercise = ""
                    freeLoad = ""
                    freeReps = ""
                    freeTempo = ""
                }
            }
        )
    }
}

@Composable
fun SessionScreenContent(
    isFreeMode: Boolean,
    stepHeader: String,
    // Active Step
    activeStepName: String?,
    activeStepReps: String?,
    activeStepDuration: String?,
    activeStepLoad: String?,
    activeStepTempo: String?,
    // Metronome
    isMetronomeEnabled: Boolean,
    effectiveTempo: String?,
    onToggleMetronome: () -> Unit,
    // Timer
    isTimerRunning: Boolean,
    timerStartTime: Long?,
    timerAccumulatedTime: Long,
    onTimerToggle: () -> Unit,
    onTimerReset: () -> Unit,
    // Actions
    onCompleteStep: () -> Unit,
    onSkipStep: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onFinishWorkout: () -> Unit,
    // Free Entry
    freeExercise: String,
    onFreeExerciseChange: (String) -> Unit,
    freeLoad: String,
    onFreeLoadChange: (String) -> Unit,
    freeReps: String,
    onFreeRepsChange: (String) -> Unit,
    freeTempo: String,
    onFreeTempoChange: (String) -> Unit,
    onLogFreeEntry: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    // Metronome Logic
    LaunchedEffect(effectiveTempo, isMetronomeEnabled, lifecycleState) {
        if (isMetronomeEnabled &&
            lifecycleState == Lifecycle.State.RESUMED &&
            effectiveTempo != null
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stepHeader,
                        style = MaterialTheme.typography.labelLarge
                    )

                    if (!isFreeMode && activeStepName != null) {
                        // Active Mode Display
                        Text(
                            text = activeStepName,
                            style = MaterialTheme.typography.displaySmall
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (activeStepReps != null) {
                                    Text("Reps: $activeStepReps", style = MaterialTheme.typography.headlineMedium)
                                }
                                if (activeStepDuration != null) {
                                    Text("Duration: ${activeStepDuration}s", style = MaterialTheme.typography.headlineMedium)
                                }
                                if (activeStepLoad != null) {
                                    Text("Load: $activeStepLoad", style = MaterialTheme.typography.titleLarge)
                                }
                                if (activeStepTempo != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TempoDisplay(activeStepTempo)
                                }
                            }
                        }
                    } else {
                        // Free Entry Mode Inputs
                        OutlinedTextField(
                            value = freeExercise,
                            onValueChange = onFreeExerciseChange,
                            label = { Text("Exercise Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = freeLoad,
                                onValueChange = onFreeLoadChange,
                                label = { Text("Load") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = freeReps,
                                onValueChange = onFreeRepsChange,
                                label = { Text("Reps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = freeTempo,
                            onValueChange = onFreeTempoChange,
                            label = { Text("Tempo (e.g. 3030)") },
                            isError = freeTempo.isNotEmpty() && freeTempo.length != 4,
                            supportingText = {
                                if (freeTempo.isNotEmpty() && freeTempo.length != 4) {
                                    Text("Must be exactly 4 digits")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Action Button
                    Button(
                        onClick = if (isFreeMode) onLogFreeEntry else onCompleteStep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text(
                            text = if (isFreeMode) "Log Set" else "COMPLETE NEXT STEP",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Buttons
                    if (!isFreeMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {}, enabled = false) { Text("Edit") }
                            Button(onClick = onSkipStep) { Text("Skip") }
                            Button(onClick = {}, enabled = false) { Text("Swap") }
                        }
                    } else {
                        // Finish Workout Button for Free Mode
                        Button(
                            onClick = onFinishWorkout,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done (Finish Workout)")
                        }
                    }

                    // Undo Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onUndo,
                            enabled = canUndo
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

        // Metronome Toggle (Top Right)
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
