package com.chrislentner.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.planner.WorkoutStep
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestScheduleScreen(
    navController: NavController,
    viewModel: SuggestScheduleViewModel
) {
    val plans = viewModel.suggestedPlans
    val isLoading = viewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggested Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(
                    items = plans,
                    key = { it.date.toEpochDay() }
                ) { plan ->
                    SuggestedDayRow(plan)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SuggestedDayRow(plan: SuggestedDayPlan) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plan.date.format(dateFormatter),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )

            if (plan.isRestDay) {
                Text(
                    text = "Rest Day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.location ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${plan.steps.size} Exercises",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded && !plan.isRestDay) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                plan.steps.forEach { step ->
                    StepRow(step)
                }
            }
        }
    }
}

@Composable
fun StepRow(step: WorkoutStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = step.exerciseName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        val details = StringBuilder()
        if (step.targetReps != null && step.targetReps > 0) {
            details.append("${step.targetReps} reps")
        } else if (step.targetDurationSeconds != null) {
            details.append("${step.targetDurationSeconds}s")
        }

        if (step.loadDescription.isNotEmpty()) {
            if (details.isNotEmpty()) details.append(" @ ")
            details.append(step.loadDescription)
        }

        if (step.tempo != null) {
             if (details.isNotEmpty()) details.append(" (${step.tempo})")
             else details.append(step.tempo)
        }

        Text(
            text = details.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
