package com.chrislentner.coach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseEntryForm(
    exerciseName: String,
    onExerciseNameChange: (String) -> Unit,
    load: String,
    onLoadChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit,
    tempo: String,
    onTempoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onExerciseNameClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = exerciseName,
            onValueChange = {
                if (onExerciseNameClick == null) {
                    onExerciseNameChange(it)
                }
            },
            label = { Text("Exercise Name") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onExerciseNameClick != null) {
                    onExerciseNameClick?.invoke()
                },
            readOnly = (onExerciseNameClick != null)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = load,
                onValueChange = onLoadChange,
                label = { Text("Load") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reps,
                onValueChange = onRepsChange,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = tempo,
            onValueChange = onTempoChange,
            label = { Text("Tempo (e.g. 3030)") },
            isError = tempo.isNotEmpty() && tempo.length != 4,
            supportingText = {
                if (tempo.isNotEmpty() && tempo.length != 4) {
                    Text("Must be exactly 4 digits")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
