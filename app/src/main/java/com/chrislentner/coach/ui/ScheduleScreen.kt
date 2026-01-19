package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.WorkoutScheduler
import com.chrislentner.coach.planner.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class ScheduleUiState {
    object Loading : ScheduleUiState()
    data class Success(val schedule: Schedule) : ScheduleUiState()
}

class ScheduleViewModel(
    private val repository: WorkoutRepository,
    private val scheduler: WorkoutScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        generateSchedule()
    }

    private fun generateSchedule() {
        viewModelScope.launch {
            val history = repository.getHistorySince(0)
            val schedule = scheduler.generateSchedule(history)
            _uiState.value = ScheduleUiState.Success(schedule)
        }
    }
}

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is ScheduleUiState.Loading -> Text(text = "Generating schedule...")
            is ScheduleUiState.Success -> {
                Column {
                    Text(
                        text = "Suggested Schedule",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    for (session in state.schedule.sessions) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(session.date)
                        Text(text = "Date: $date")
                        Text(text = "Location: ${session.location}")
                        Text(text = "Duration: ${session.durationMinutes} minutes")
                        Text(text = "Blocks: ${session.blocks.joinToString { it.blockName }}")
                        Text(text = "")
                    }
                }
            }
        }
    }
}

class ScheduleViewModelFactory(
    private val repository: WorkoutRepository,
    private val scheduler: WorkoutScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
