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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
            viewModel.exerciseName = it
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
        ) {
            ExerciseEntryForm(
                exerciseName = viewModel.exerciseName,
                onExerciseClick = { navController.navigate("exercise_selection") },
                load = viewModel.load,
                onLoadChange = { viewModel.load = it },
                reps = viewModel.reps,
                onRepsChange = { viewModel.reps = it },
                tempo = viewModel.tempo,
                onTempoChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        viewModel.tempo = it
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

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
        }
    }
}
