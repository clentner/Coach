package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    navController: NavController,
    viewModel: StatusViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status & Deficits") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Targets",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(state.targets) { target ->
                    TargetItem(target)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fatigue Constraints",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(state.fatigues) { fatigue ->
                    FatigueItem(fatigue)
                }
            }
        }
    }
}

@Composable
fun TargetItem(target: TargetStatus) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatTargetName(target.id),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val progress = (target.performed / target.goal).coerceIn(0.0, 1.0).toFloat()
            val label = if (target.type == "minutes") {
                "${target.performed.toInt()} / ${target.goal.toInt()} mins"
            } else {
                "${target.performed.toInt()} / ${target.goal.toInt()} sets"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text(text = label, style = MaterialTheme.typography.bodyMedium)
                 Text(
                     text = "${target.windowDays} days",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
            )

            if (target.deficit > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deficit: ${formatDeficit(target.deficit, target.type)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun FatigueItem(fatigue: FatigueStatus) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (fatigue.isOk) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha=0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = fatigue.kind.replace("_", " ").capitalizeWords(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (fatigue.isOk) "OK" else "Exceeded",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (fatigue.isOk) Color.Green else Color.Red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = fatigue.reason, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            val progress = (fatigue.currentLoad / fatigue.threshold).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = if (fatigue.isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Load: ${String.format("%.2f", fatigue.currentLoad)} / ${fatigue.threshold} (${fatigue.windowHours}h)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatTargetName(id: String): String {
    return id.replace("_", " ").capitalizeWords()
}

fun formatDeficit(deficit: Double, type: String): String {
    return if (type == "minutes") {
        "${deficit.toInt()} mins"
    } else {
        "${deficit.toInt()} sets"
    }
}

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
