package com.chrislentner.coach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun RestTimer(
    isRunning: Boolean,
    timerStartTime: Long?,
    timerAccumulatedTime: Long,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive) {
                now = System.currentTimeMillis()
                delay(100)
            }
        }
    }

    val totalElapsed = if (isRunning && timerStartTime != null) {
        (now - timerStartTime) + timerAccumulatedTime
    } else {
        timerAccumulatedTime
    }

    val seconds = (totalElapsed / 1000) % 60
    val minutes = (totalElapsed / 1000) / 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggle) {
                    if (isRunning) {
                        val color = MaterialTheme.colorScheme.onSecondaryContainer
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val barWidth = size.width / 3
                            drawRect(
                                color = color,
                                topLeft = Offset(0f, 0f),
                                size = Size(barWidth, size.height)
                            )
                            drawRect(
                                color = color,
                                topLeft = Offset(size.width - barWidth, 0f),
                                size = Size(barWidth, size.height)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start"
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset"
                    )
                }
            }
        }
    }
}
