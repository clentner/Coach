package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun DeficitAndFatigueScreen(
    navController: NavController,
    viewModel: DeficitAndFatigueViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Deficits", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        uiState.deficits.forEach { (name, value) ->
            Text("$name: $value")
        }

        Text("Fatigue", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        uiState.fatigue.forEach { (name, value) ->
            Text("$name: $value")
        }
    }
}
