package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedScheduleScreen(
    navController: NavController,
    viewModel: SuggestedScheduleViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggested Schedule") },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Building your 7-day plan...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            viewModel.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(viewModel.errorMessage ?: "Unable to build plan.")
                }
            }
            else -> {
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

                    itemsIndexed(viewModel.dayPlans) { index, plan ->
                        SessionRow(
                            session = plan.summary,
                            formattedDate = viewModel.formatDate(plan.summary.date),
                            onClick = { navController.navigate("suggested_schedule_detail/$index") }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedScheduleDetailScreen(
    navController: NavController,
    viewModel: SuggestedScheduleViewModel,
    dayIndex: Int
) {
    val plan = viewModel.getPlanForDay(dayIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggested Sets") },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (plan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Plan not found.")
            }
        } else {
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
                        Text("Exercise", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("Load", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Target", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider()
                }

                itemsIndexed(plan.logs) { _, log ->
                    LogEntryRow(log = log, onClick = {})
                    HorizontalDivider()
                }
            }
        }
    }
}
