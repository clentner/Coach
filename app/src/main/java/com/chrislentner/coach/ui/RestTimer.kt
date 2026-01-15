package com.chrislentner.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RestTimer(
    isRunning: Boolean,
    startTime: Long?,
    accumulatedTime: Long,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state to drive UI updates when running
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRunning, startTime) {
        if (isRunning) {
            while (true) {
                now = System.currentTimeMillis()
                delay(100) // 10Hz update
            }
        }
    }

    val elapsedMillis = if (isRunning && startTime != null) {
        (now - startTime) + accumulatedTime
    } else {
        accumulatedTime
    }

    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant) // Distinct background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rest Timer",
            style = MaterialTheme.typography.labelMedium
        )

        Text(
            text = timeString,
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onToggle) {
                Text(if (isRunning) "Pause" else "Start")
            }

            OutlinedButton(onClick = onReset) {
                Text("Reset")
            }
        }
    }
}
