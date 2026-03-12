package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    navController: NavController,
    viewModel: EditExerciseViewModel
) {
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val selectedExerciseFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("selected_exercise", null)
    }
    val selectedExercise by selectedExerciseFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(selectedExercise) {
        selectedExercise?.let {
            viewModel.onExerciseSelected(it)
            savedStateHandle?.remove<String>("selected_exercise")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ExerciseEntryForm(
                exerciseName = viewModel.exerciseName,
                onExerciseClick = { navController.navigate("exercise_selection") },
                load = viewModel.load,
                onLoadChange = { viewModel.load = it },
                reps = viewModel.reps,
                onRepsChange = { viewModel.reps = it },
                duration = viewModel.durationMinutes,
                onDurationChange = { viewModel.durationMinutes = it },
                tempo = viewModel.tempo,
                onTempoChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        viewModel.tempo = it
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Targets & Fatigue section
            if (viewModel.planner != null) {
                var showAddTargetDialog by remember { mutableStateOf(false) }
                var showAddFatigueDialog by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Targets", style = MaterialTheme.typography.titleMedium)
                    viewModel.customTargets.forEach { (id, value) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = "$id: $value", modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.removeTarget(id) }) {
                                Text("Remove")
                            }
                        }
                    }
                    Button(onClick = { showAddTargetDialog = true }) {
                        Text("Add Target")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Fatigue Constraints", style = MaterialTheme.typography.titleMedium)
                    viewModel.customFatigue.forEach { (kind, value) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = "$kind: $value", modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.removeFatigue(kind) }) {
                                Text("Remove")
                            }
                        }
                    }
                    Button(onClick = { showAddFatigueDialog = true }) {
                        Text("Add Fatigue")
                    }
                }

                if (showAddTargetDialog) {
                    var selectedTargetId by remember { mutableStateOf(viewModel.allTargetIds.firstOrNull() ?: "") }
                    var targetAmount by remember { mutableStateOf(viewModel.getDefaultTargetValue(selectedTargetId).toString()) }
                    var expanded by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showAddTargetDialog = false },
                        title = { Text("Add Target") },
                        text = {
                            Column {
                                Box {
                                    OutlinedTextField(
                                        value = selectedTargetId,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Target") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier.matchParentSize().clickable { expanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        viewModel.allTargetIds.forEach { id ->
                                            DropdownMenuItem(
                                                text = { Text(id) },
                                                onClick = {
                                                    selectedTargetId = id
                                                    targetAmount = viewModel.getDefaultTargetValue(id).toString()
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = targetAmount,
                                    onValueChange = { targetAmount = it },
                                    label = { Text("Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val amount = targetAmount.toDoubleOrNull()
                                if (amount != null && selectedTargetId.isNotEmpty()) {
                                    viewModel.addTarget(selectedTargetId, amount)
                                }
                                showAddTargetDialog = false
                            }) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddTargetDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showAddFatigueDialog) {
                    var selectedFatigueKind by remember { mutableStateOf(viewModel.allFatigueKinds.firstOrNull() ?: "") }
                    var fatigueAmount by remember { mutableStateOf(viewModel.getDefaultFatigueValue(selectedFatigueKind).toString()) }
                    var expanded by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showAddFatigueDialog = false },
                        title = { Text("Add Fatigue") },
                        text = {
                            Column {
                                Box {
                                    OutlinedTextField(
                                        value = selectedFatigueKind,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Fatigue Kind") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier.matchParentSize().clickable { expanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        viewModel.allFatigueKinds.forEach { kind ->
                                            DropdownMenuItem(
                                                text = { Text(kind) },
                                                onClick = {
                                                    selectedFatigueKind = kind
                                                    fatigueAmount = viewModel.getDefaultFatigueValue(kind).toString()
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = fatigueAmount,
                                    onValueChange = { fatigueAmount = it },
                                    label = { Text("Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val amount = fatigueAmount.toDoubleOrNull()
                                if (amount != null && selectedFatigueKind.isNotEmpty()) {
                                    viewModel.addFatigue(selectedFatigueKind, amount)
                                }
                                showAddFatigueDialog = false
                            }) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddFatigueDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.save(onSuccess = {
                        navController.popBackStack()
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save")
            }

            if (viewModel.isEditing) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.delete(onSuccess = {
                            navController.popBackStack()
                        })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
