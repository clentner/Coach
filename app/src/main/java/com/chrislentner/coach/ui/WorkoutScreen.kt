package com.chrislentner.coach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.chrislentner.coach.planner.LoadLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel
) {
    val uiState = viewModel.uiState

    // Handle Exercise Swap Result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val selectedExerciseFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("selected_exercise", null)
    }
    val selectedExercise by selectedExerciseFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    // Free Entry State (Hoisted to share with Metronome logic)
    var freeExercise by remember { mutableStateOf("") }
    var freeLoad by remember { mutableStateOf("") }
    var freeReps by remember { mutableStateOf("") }
    var freeTempo by remember { mutableStateOf("") }

    LaunchedEffect(selectedExercise) {
        selectedExercise?.let {
            if (uiState is WorkoutUiState.FreeEntry) {
                freeExercise = it
            } else {
                viewModel.updateCurrentStepExercise(it)
            }
            savedStateHandle?.remove<String>("selected_exercise")
        }
    }

    // Calculate effective tempo for metronome
    val effectiveTempo = when (uiState) {
        is WorkoutUiState.Active -> uiState.currentStep?.tempo
        is WorkoutUiState.FreeEntry -> if (freeTempo.length == 4 && freeTempo.all { it.isDigit() }) freeTempo else null
        else -> null
    }

    val hasTempo = effectiveTempo != null
    val isMetronomeActive = if (hasTempo) {
        viewModel.isMetronomeEnabledWithTempo
    } else {
        viewModel.isMetronomeEnabledWithoutTempo
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
            // Editing Callbacks
            onAdjustReps = { viewModel.adjustCurrentStepReps(it) },
            onUpdateReps = { viewModel.updateCurrentStepReps(it) },
            onAdjustLoad = { viewModel.adjustCurrentStepLoad(it) },
            onUpdateLoad = { viewModel.updateCurrentStepLoad(it) },
            onUpdateTempo = { viewModel.updateCurrentStepTempo(it) },
            onSwapExercise = { navController.navigate("exercise_selection") },
            // Metronome
            isMetronomeEnabled = isMetronomeActive,
            effectiveTempo = effectiveTempo,
            onToggleMetronome = { viewModel.toggleMetronome(hasTempo) },
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
    // Editing Actions
    onAdjustReps: (Int) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onAdjustLoad: (Boolean) -> Unit, // True = Increment, False = Decrement
    onUpdateLoad: (String) -> Unit,
    onUpdateTempo: (String?) -> Unit,
    onSwapExercise: () -> Unit,
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
    LaunchedEffect(isMetronomeEnabled, lifecycleState) {
        if (isMetronomeEnabled &&
            lifecycleState == Lifecycle.State.RESUMED
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
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.clickable { onSwapExercise() }
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
                                    EditableField(
                                        value = activeStepReps,
                                        label = "Reps",
                                        onValueChange = { str ->
                                            str.toIntOrNull()?.let { onUpdateReps(it) }
                                        },
                                        onIncrement = { onAdjustReps(1) },
                                        onDecrement = { onAdjustReps(-1) },
                                        keyboardType = KeyboardType.Number
                                    )
                                }
                                if (activeStepDuration != null) {
                                    Text("Duration: ${activeStepDuration}s", style = MaterialTheme.typography.headlineMedium)
                                }
                                if (activeStepLoad != null) {
                                    EditableField(
                                        value = activeStepLoad,
                                        label = "Load",
                                        onValueChange = { onUpdateLoad(it) },
                                        onIncrement = { onAdjustLoad(true) },
                                        onDecrement = { onAdjustLoad(false) },
                                        canAdjust = LoadLogic.hasNumericComponent(activeStepLoad),
                                        keyboardType = KeyboardType.Text // Allows freeform
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TempoEditableField(
                                    tempo = activeStepTempo,
                                    onUpdateTempo = onUpdateTempo
                                )
                            }
                        }
                    } else {
                        // Free Entry Mode Inputs
                        ExerciseEntryForm(
                            exerciseName = freeExercise,
                            onExerciseClick = onSwapExercise,
                            load = freeLoad,
                            onLoadChange = onFreeLoadChange,
                            reps = freeReps,
                            onRepsChange = onFreeRepsChange,
                            tempo = freeTempo,
                            onTempoChange = onFreeTempoChange,
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
                            Button(onClick = onSkipStep) { Text("Skip") }
                            Button(
                                onClick = onUndo,
                                enabled = canUndo
                            ) { Text("Undo") }
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

                        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
fun InlineInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    isValid: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            isError = !isValid
        )
        IconButton(
            onClick = onAccept,
            enabled = isValid
        ) {
            Icon(Icons.Default.Check, contentDescription = "Accept")
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Discard")
        }
    }
}

@Composable
fun TempoEditableField(
    tempo: String?,
    onUpdateTempo: (String?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(tempo ?: "") }

    LaunchedEffect(tempo) {
        if (!isEditing) {
            tempValue = tempo ?: ""
        }
    }

    if (isEditing) {
        val isValid = tempValue.isEmpty() || (tempValue.length == 4 && tempValue.all { it.isDigit() })

        InlineInputRow(
            value = tempValue,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    tempValue = newValue
                }
            },
            label = "Tempo (e.g. 3030)",
            onAccept = {
                if (isValid) {
                    onUpdateTempo(if (tempValue.isEmpty()) null else tempValue)
                    isEditing = false
                }
            },
            onCancel = {
                tempValue = tempo ?: ""
                isEditing = false
            },
            isValid = isValid,
            keyboardType = KeyboardType.Number
        )
    } else {
        Box(
            modifier = Modifier
                .clickable { isEditing = true }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (tempo != null) {
                TempoDisplay(tempo)
            } else {
                Text(
                    text = "No Tempo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EditableField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canAdjust: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    // Sync tempValue if value changes externally
    LaunchedEffect(value) {
        if (!isEditing) {
            tempValue = value
        }
    }

    if (isEditing) {
        InlineInputRow(
            value = tempValue,
            onValueChange = { tempValue = it },
            label = label,
            onAccept = {
                onValueChange(tempValue)
                isEditing = false
            },
            onCancel = {
                tempValue = value
                isEditing = false
            },
            keyboardType = keyboardType
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (canAdjust) {
                IconButton(onClick = onDecrement) {
                    Text("-", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Text(
                text = if (label == "Reps") "Reps: $value" else "Load: $value",
                style = if (label == "Reps") MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { isEditing = true }
            )

            if (canAdjust) {
                IconButton(onClick = onIncrement) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}
