package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val repository: WorkoutRepository,
    val sessionId: Long
) : ViewModel() {

    var logs by mutableStateOf<List<WorkoutLogEntry>>(emptyList())
        private set

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            logs = repository.getLogsForSession(sessionId)
        }
    }
}

class WorkoutDetailViewModelFactory(
    private val repository: WorkoutRepository,
    private val sessionId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutDetailViewModel(repository, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
