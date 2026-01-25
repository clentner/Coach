package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.WorkoutStep
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class WorkoutUiState {
    object Loading : WorkoutUiState()
    data class Active(
        val session: WorkoutSession,
        val currentStep: WorkoutStep?,
        val remainingSteps: List<WorkoutStep>,
        val completedStepsCount: Int,
        val totalStepsCount: Int
    ) : WorkoutUiState()
    data class FreeEntry(val session: WorkoutSession) : WorkoutUiState()
}

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository? = null,
    private val planner: AdvancedWorkoutPlanner? = null // Optional for incremental migration/testing
) : ViewModel() {

    var uiState by mutableStateOf<WorkoutUiState>(WorkoutUiState.Loading)
        private set

    var isMetronomeEnabledWithTempo by mutableStateOf(true)
        private set

    var isMetronomeEnabledWithoutTempo by mutableStateOf(false)
        private set

    var isTimerRunning by mutableStateOf(false)
        private set

    var timerStartTime by mutableStateOf<Long?>(null)
        private set

    var timerAccumulatedTime by mutableStateOf(0L)
        private set

    private var cachedPlan: List<WorkoutStep>? = null

    init {
        initializeSession()
    }

    fun toggleMetronome(hasTempo: Boolean) {
        if (hasTempo) {
            isMetronomeEnabledWithTempo = !isMetronomeEnabledWithTempo
        } else {
            isMetronomeEnabledWithoutTempo = !isMetronomeEnabledWithoutTempo
        }
    }

    fun toggleTimer() {
        if (isTimerRunning) {
            // Pause
            val now = System.currentTimeMillis()
            val elapsed = now - (timerStartTime ?: now)
            timerAccumulatedTime += elapsed
            timerStartTime = null
            isTimerRunning = false
        } else {
            // Start
            timerStartTime = System.currentTimeMillis()
            isTimerRunning = true
        }
    }

    fun resetTimer() {
        isTimerRunning = false
        timerStartTime = null
        timerAccumulatedTime = 0L
    }

    private fun initializeSession() {
        viewModelScope.launch {
            val now = Instant.now()
            val today = LocalDate.now()
            val todayStr = today.toString()

            // 0. Get scheduled location
            var location: String? = null
            if (scheduleRepository != null) {
                val schedule = scheduleRepository.getScheduleByDate(todayStr)
                location = schedule?.location
            }

            // 1. Get or Create Session
            val session = repository.getOrCreateSession(todayStr, now.toEpochMilli(), location)

            // 2. Fetch History & Generate Plan (Only if not already cached)
            if (cachedPlan == null) {
                val historyCutoff = today.minusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val rawHistory = repository.getHistorySince(historyCutoff)
                val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Exclude today's logs for consistent planning
                val historyForPlanning = rawHistory.filter { it.timestamp < startOfToday }

                cachedPlan = if (planner != null && scheduleRepository != null) {
                    val schedule = scheduleRepository.getScheduleByDate(todayStr)
                    if (schedule != null) {
                        planner.generatePlan(now, historyForPlanning, schedule).steps
                    } else {
                        emptyList()
                    }
                } else {
                    com.chrislentner.coach.planner.WorkoutPlanner.generatePlan(today, historyForPlanning)
                }
            }

            val fullPlan = cachedPlan!!

            // 3. Filter out completed sets for THIS session
            val sessionLogs = repository.getLogsForSession(session.id)
            val completedCount = sessionLogs.size

            if (completedCount >= fullPlan.size) {
                uiState = WorkoutUiState.FreeEntry(session)
            } else {
                val remaining = fullPlan.drop(completedCount)
                uiState = WorkoutUiState.Active(
                    session = session,
                    currentStep = remaining.firstOrNull(),
                    remainingSteps = remaining.drop(1),
                    completedStepsCount = completedCount,
                    totalStepsCount = fullPlan.size
                )
            }
        }
    }

    fun completeCurrentStep() {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            viewModelScope.launch {
                val step = state.currentStep
                val entry = WorkoutLogEntry(
                    sessionId = state.session.id,
                    exerciseName = step.exerciseName,
                    targetReps = step.targetReps,
                    targetDurationSeconds = step.targetDurationSeconds,
                    loadDescription = step.loadDescription,
                    tempo = step.tempo,
                    actualReps = step.targetReps, // Defaulting to target
                    actualDurationSeconds = step.targetDurationSeconds, // Defaulting
                    rpe = null,
                    notes = null,
                    skipped = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.logSet(entry)

                // Refresh state
                // We could just optimistically update, but re-running init is safer to stay in sync
                initializeSession()

                // Reset and start timer
                resetTimer()
                toggleTimer()
            }
        }
    }

    fun skipCurrentStep() {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            viewModelScope.launch {
                val step = state.currentStep
                val entry = WorkoutLogEntry(
                    sessionId = state.session.id,
                    exerciseName = step.exerciseName,
                    targetReps = step.targetReps,
                    targetDurationSeconds = step.targetDurationSeconds,
                    loadDescription = step.loadDescription,
                    tempo = step.tempo,
                    actualReps = step.targetReps, // Copy target
                    actualDurationSeconds = step.targetDurationSeconds, // Copy target
                    rpe = null,
                    notes = null,
                    skipped = true,
                    timestamp = System.currentTimeMillis()
                )
                repository.logSet(entry)

                initializeSession()

                // Timer should NOT be reset or started on skip
            }
        }
    }

    fun logFreeEntry(exerciseName: String, load: String, reps: String, tempo: String?) {
        val state = uiState
        if (state is WorkoutUiState.FreeEntry) {
             viewModelScope.launch {
                val entry = WorkoutLogEntry(
                    sessionId = state.session.id,
                    exerciseName = exerciseName,
                    targetReps = reps.toIntOrNull(),
                    targetDurationSeconds = null,
                    loadDescription = load,
                    tempo = tempo,
                    actualReps = reps.toIntOrNull(),
                    actualDurationSeconds = null,
                    rpe = null,
                    notes = "Free Entry",
                    skipped = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.logSet(entry)
                // Stay in Free Entry mode, maybe show a toast or clear inputs?
                // For now, just logging it.
            }
        }
    }

    fun undoLastStep() {
        val state = uiState
        val sessionId = when (state) {
            is WorkoutUiState.Active -> state.session.id
            is WorkoutUiState.FreeEntry -> state.session.id
            else -> return
        }

        viewModelScope.launch {
            val logs = repository.getLogsForSession(sessionId)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                repository.deleteLog(lastLog)
                initializeSession()
            }
        }
    }

    fun updateCurrentStepExercise(newName: String) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val newStep = state.currentStep.copy(exerciseName = newName)
            uiState = state.copy(currentStep = newStep)
        }
    }

    fun updateCurrentStepLoad(newLoad: String) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val newStep = state.currentStep.copy(loadDescription = newLoad)
            uiState = state.copy(currentStep = newStep)
        }
    }

    fun updateCurrentStepReps(newReps: Int) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val newStep = state.currentStep.copy(targetReps = newReps)
            uiState = state.copy(currentStep = newStep)
        }
    }

    fun updateCurrentStepTempo(newTempo: String?) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val newStep = state.currentStep.copy(tempo = newTempo)
            uiState = state.copy(currentStep = newStep)
        }
    }

    fun adjustCurrentStepLoad(increment: Boolean) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val currentLoad = state.currentStep.loadDescription
            val newLoad = com.chrislentner.coach.planner.LoadLogic.adjustLoad(currentLoad, increment)
            updateCurrentStepLoad(newLoad)
        }
    }

    fun adjustCurrentStepReps(increment: Int) {
        val state = uiState
        if (state is WorkoutUiState.Active && state.currentStep != null) {
            val currentReps = state.currentStep.targetReps ?: 0
            val newReps = (currentReps + increment).coerceAtLeast(1)
            updateCurrentStepReps(newReps)
        }
    }
}

// Factory to inject Repo
class WorkoutViewModelFactory(
    private val repository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: AdvancedWorkoutPlanner?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(repository, scheduleRepository, planner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
