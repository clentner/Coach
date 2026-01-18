package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricalWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long?
) : ViewModel() {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session = _session.asStateFlow()

    init {
        viewModelScope.launch {
            _session.value = if (sessionId != null) {
                repository.getLogsForSession(sessionId).firstOrNull()?.let { log ->
                    repository.getOrCreateSession(
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(log.timestamp)),
                        log.timestamp
                    )
                }
            } else {
                null // A new session will be created on the first log.
            }
        }
    }

    fun logSet(
        date: Date,
        exerciseName: String,
        load: String,
        reps: String,
        tempo: String?,
        onSessionCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val sessionToLog = _session.value ?: run {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                val newSession = repository.getOrCreateSession(dateStr, date.time)
                _session.value = newSession
                onSessionCreated(newSession.id)
                newSession
            }

            val entry = WorkoutLogEntry(
                sessionId = sessionToLog.id,
                exerciseName = exerciseName,
                targetReps = reps.toIntOrNull(),
                targetDurationSeconds = null,
                loadDescription = load,
                tempo = tempo,
                actualReps = reps.toIntOrNull(),
                actualDurationSeconds = null,
                rpe = null,
                notes = "Historical Entry",
                skipped = false,
                timestamp = date.time
            )
            repository.logSet(entry)
        }
    }
}

class HistoricalWorkoutViewModelFactory(
    private val repository: WorkoutRepository,
    private val sessionId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricalWorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoricalWorkoutViewModel(repository, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
