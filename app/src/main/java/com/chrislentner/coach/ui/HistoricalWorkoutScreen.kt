package com.chrislentner.coach.ui

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalWorkoutScreen(
    navController: NavController,
    viewModel: HistoricalWorkoutViewModel
) {
    var freeExercise by remember { mutableStateOf("") }
    var freeLoad by remember { mutableStateOf("") }
    var freeReps by remember { mutableStateOf("") }
    var freeTempo by remember { mutableStateOf("") }

    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                date = calendar.time
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Past Workout") },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)}",
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Change Date")
                }
            }

            SessionScreenContent(
                isFreeMode = true,
                stepHeader = "Free Entry",
                // Active Step Data (not used)
                activeStepName = null,
                activeStepReps = null,
                activeStepDuration = null,
                activeStepLoad = null,
                activeStepTempo = null,
                // Editing Callbacks (not used)
                onAdjustReps = {},
                onUpdateReps = {},
                onAdjustLoad = {},
                onUpdateLoad = {},
                onUpdateTempo = {},
                onSwapExercise = {},
                // Metronome (not used)
                isMetronomeEnabled = false,
                effectiveTempo = null,
                onToggleMetronome = {},
                // Timer (not used)
                isTimerRunning = false,
                timerStartTime = null,
                timerAccumulatedTime = 0L,
                onTimerToggle = {},
                onTimerReset = {},
                // Actions
                onCompleteStep = {},
                onSkipStep = {},
                onUndo = {}, // Consider adding undo for historical entries
                canUndo = false,
                onFinishWorkout = { navController.popBackStack() },
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
                        viewModel.logSet(
                            date,
                            freeExercise,
                            freeLoad,
                            freeReps,
                            if (freeTempo.length == 4) freeTempo else null
                        ) { newSessionId ->
                            // Optional: Could navigate to the new detail screen
                        }
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
}
