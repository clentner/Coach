package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutLogEntry
import androidx.compose.runtime.mutableStateMapOf
import com.chrislentner.coach.database.WorkoutRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class EditExerciseViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
    private val logId: Long?,
    val planner: AdvancedWorkoutPlanner?
) : ViewModel() {

    var exerciseName by mutableStateOf("")
    var load by mutableStateOf("")
    var reps by mutableStateOf("")
    var durationMinutes by mutableStateOf("")
    var tempo by mutableStateOf("")

    val customTargets = mutableStateMapOf<String, Double>()
    val customFatigue = mutableStateMapOf<String, Double>()

    val isEditing: Boolean
        get() = logId != null && logId != -1L

    private var existingLog: WorkoutLogEntry? = null

    val allTargetIds: List<String>
        get() = planner?.config?.targets?.map { it.id } ?: emptyList()

    val allFatigueKinds: List<String>
        get() = planner?.config?.fatigueConstraints?.keys?.toList() ?: emptyList()

    init {
        if (isEditing) {
            viewModelScope.launch {
                val log = repository.getLogById(logId!!)
                if (log != null) {
                    existingLog = log
                    exerciseName = log.exerciseName
                    load = log.loadDescription
                    reps = log.targetReps?.toString() ?: ""
                    durationMinutes = log.targetDurationSeconds?.let { (it / 60).toString() } ?: ""
                    tempo = log.tempo ?: ""

                    try {
                        val mapper = jacksonObjectMapper()
                        if (!log.customTargets.isNullOrBlank()) {
                            val targets: Map<String, Double> = mapper.readValue(log.customTargets)
                            customTargets.putAll(targets)
                        }
                        if (!log.customFatigue.isNullOrBlank()) {
                            val fatigue: Map<String, Double> = mapper.readValue(log.customFatigue)
                            customFatigue.putAll(fatigue)
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
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
                durationMinutes = lastLog.targetDurationSeconds?.let { (it / 60).toString() } ?: ""
                tempo = lastLog.tempo ?: ""
            }
        }
    }

    fun addTarget(id: String, value: Double) {
        customTargets[id] = value
    }

    fun removeTarget(id: String) {
        customTargets.remove(id)
    }

    fun addFatigue(kind: String, value: Double) {
        customFatigue[kind] = value
    }

    fun removeFatigue(kind: String) {
        customFatigue.remove(kind)
    }

    fun getDefaultTargetValue(id: String): Double {
        val target = planner?.config?.targets?.find { it.id == id }
        if (target != null && (target.type.endsWith("sets", ignoreCase = true) || target.type.endsWith("set", ignoreCase = true))) {
            return 1.0
        }
        return target?.goal?.toDouble() ?: 0.0
    }

    fun getDefaultFatigueValue(kind: String): Double {
        // Fallback to first constraint if exercise doesn't have a default
        val constraint = planner?.config?.fatigueConstraints?.get(kind)?.firstOrNull()
        return constraint?.threshold ?: 1.0
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val repsInt = reps.toIntOrNull()
            val durationSecondsInt = durationMinutes.toIntOrNull()?.let { it * 60 }
            val validTempo = if (tempo.length == 4 && tempo.all { it.isDigit() }) tempo else null

            val mapper = jacksonObjectMapper()
            val customTargetsStr = if (customTargets.isNotEmpty()) mapper.writeValueAsString(customTargets) else null
            val customFatigueStr = if (customFatigue.isNotEmpty()) mapper.writeValueAsString(customFatigue) else null

            if (existingLog != null) {
                // Update existing
                val updated = existingLog!!.copy(
                    exerciseName = exerciseName,
                    loadDescription = load,
                    targetReps = repsInt,
                    actualReps = repsInt,
                    targetDurationSeconds = durationSecondsInt,
                    actualDurationSeconds = durationSecondsInt,
                    tempo = validTempo,
                    customTargets = customTargetsStr,
                    customFatigue = customFatigueStr
                )
                repository.updateLog(updated)
            } else {
                val session = repository.getSessionById(sessionId)
                val isToday = session != null && session.date == LocalDate.now(ZoneId.systemDefault()).toString()
                val timestamp = if (session != null && !isToday) {
                    session.startTimeInMillis
                } else {
                    System.currentTimeMillis()
                }

                val newEntry = WorkoutLogEntry(
                    sessionId = sessionId,
                    exerciseName = exerciseName,
                    targetReps = repsInt,
                    targetDurationSeconds = durationSecondsInt,
                    loadDescription = load,
                    tempo = validTempo,
                    actualReps = repsInt,
                    actualDurationSeconds = durationSecondsInt,
                    rpe = null,
                    notes = "Manual Entry",
                    skipped = false,
                    timestamp = timestamp,
                    customTargets = customTargetsStr,
                    customFatigue = customFatigueStr
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
    private val logId: Long?,
    private val planner: AdvancedWorkoutPlanner?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditExerciseViewModel(repository, sessionId, logId, planner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
