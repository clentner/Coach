package com.chrislentner.coach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.WorkoutPlanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionScreen(
    navController: NavController,
    repository: WorkoutRepository
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        val recents = repository.getRecentExerciseNames(20)
        val defaults = WorkoutPlanner.DEFAULT_EXERCISES
        // Prioritize recents, then defaults. Remove duplicates.
        suggestions = (recents + defaults).distinct()
    }

    // Filter suggestions based on search text
    val filteredSuggestions = remember(searchText, suggestions) {
        if (searchText.isBlank()) {
            suggestions
        } else {
            suggestions.filter { it.contains(searchText, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Exercise") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search / Input Field
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Exercise Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            // "Create" option if search text is not empty and not in list
            if (searchText.isNotBlank() && !filteredSuggestions.any { it.equals(searchText, ignoreCase = true) }) {
                ListItem(
                    headlineContent = { Text("Create \"$searchText\"") },
                    modifier = Modifier.clickable {
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_exercise", searchText)
                        navController.popBackStack()
                    }
                )
                HorizontalDivider()
            }

            // Suggestions List
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredSuggestions.isEmpty() && searchText.isBlank()) {
                   item {
                       Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                           CircularProgressIndicator()
                       }
                   }
                } else {
                    items(filteredSuggestions) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise) },
                            modifier = Modifier.clickable {
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    "selected_exercise",
                                    exercise
                                )
                                navController.popBackStack()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
