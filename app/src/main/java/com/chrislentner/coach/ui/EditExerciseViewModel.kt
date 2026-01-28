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

class EditExerciseViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
    private val logId: Long?
) : ViewModel() {

    var exerciseName by mutableStateOf("")
    var load by mutableStateOf("")
    var reps by mutableStateOf("")
    var tempo by mutableStateOf("")

    val isEditing: Boolean
        get() = logId != null && logId != -1L

    private var existingLog: WorkoutLogEntry? = null

    init {
        if (isEditing) {
            viewModelScope.launch {
                val log = repository.getLogById(logId!!)
                if (log != null) {
                    existingLog = log
                    exerciseName = log.exerciseName
                    load = log.loadDescription
                    reps = log.targetReps?.toString() ?: ""
                    tempo = log.tempo ?: ""
                }
            }
        }
    }

    fun onExerciseSelected(name: String) {
        exerciseName = name
        // Pre-fill from last log if not editing an existing log (or if we want to overwrite even when editing? Assuming overwrite is fine if user changes exercise)
        // If we are editing, and we change the exercise, we probably want to pre-fill the stats for THAT exercise.
        viewModelScope.launch {
            val lastLog = repository.getLastLogForExercise(name)
            if (lastLog != null) {
                load = lastLog.loadDescription
                reps = lastLog.targetReps?.toString() ?: ""
                tempo = lastLog.tempo ?: ""
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val repsInt = reps.toIntOrNull()
            val validTempo = if (tempo.length == 4 && tempo.all { it.isDigit() }) tempo else null

            if (existingLog != null) {
                // Update existing
                val updated = existingLog!!.copy(
                    exerciseName = exerciseName,
                    loadDescription = load,
                    targetReps = repsInt,
                    actualReps = repsInt,
                    tempo = validTempo
                )
                repository.updateLog(updated)
            } else {
                // Create new
                // Need session timestamp
                val session = repository.getSessionById(sessionId)
                val timestamp = session?.startTimeInMillis ?: System.currentTimeMillis()

                // Add a small offset to ensure it appears at the end if added now?
                // No, just use session start time. Order is by timestamp.
                // If multiple logs have same timestamp, order is undefined or by ID?
                // Log ID is auto-inc, so insertion order might matter if sorted by ID, but Query sorts by timestamp.
                // If timestamp is identical, order is unstable.
                // Maybe add current time offset?
                // session.startTime + (System.now - session.startTime) ? No that's now.
                // session.startTime + 1 hour?
                // Let's just use session.startTimeInMillis.

                val newEntry = WorkoutLogEntry(
                    sessionId = sessionId,
                    exerciseName = exerciseName,
                    targetReps = repsInt,
                    targetDurationSeconds = null,
                    loadDescription = load,
                    tempo = validTempo,
                    actualReps = repsInt,
                    actualDurationSeconds = null,
                    rpe = null,
                    notes = "Manual Entry",
                    skipped = false,
                    timestamp = timestamp
                )
                repository.logSet(newEntry)
            }
            onSuccess()
        }
    }

    fun delete(onSuccess: () -> Unit) {
        viewModelScope.launch {
            existingLog?.let {
                repository.deleteLog(it)
            }
            onSuccess()
        }
    }
}

class EditExerciseViewModelFactory(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
    private val logId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditExerciseViewModel(repository, sessionId, logId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
