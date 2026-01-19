package com.chrislentner.coach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.SessionSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastWorkoutsScreen(
    navController: NavController,
    viewModel: PastWorkoutsViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            viewModel.createSession(selectedDate) { sessionId ->
                                showDatePicker = false
                                navController.navigate("workout_detail/$sessionId")
                            }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past Workouts") },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Past Session")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Location", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Sets", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                }
                HorizontalDivider()
            }

            items(viewModel.sessions) { session ->
                SessionRow(
                    session = session,
                    formattedDate = viewModel.formatDate(session.date),
                    onClick = { navController.navigate("workout_detail/${session.id}") }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SessionRow(
    session: SessionSummary,
    formattedDate: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formattedDate, modifier = Modifier.weight(1f))
        Text(session.location ?: "Unknown", modifier = Modifier.weight(1f))
        Text("${session.setCount}", modifier = Modifier.weight(0.5f))
    }
}
