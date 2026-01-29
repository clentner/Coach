package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.max

data class TargetStatus(
    val id: String,
    val type: String, // "sets" or "minutes"
    val goal: Double,
    val performed: Double,
    val deficit: Double,
    val windowDays: Int
)

data class FatigueStatus(
    val kind: String,
    val currentLoad: Double,
    val threshold: Double,
    val windowHours: Int,
    val isOk: Boolean,
    val reason: String
)

data class StatusState(
    val targets: List<TargetStatus> = emptyList(),
    val fatigues: List<FatigueStatus> = emptyList(),
    val isLoading: Boolean = true
)

class StatusViewModel(
    private val repository: WorkoutRepository,
    private val planner: AdvancedWorkoutPlanner
) : ViewModel() {

    private val _state = MutableStateFlow(StatusState())
    val state: StateFlow<StatusState> = _state

    init {
        loadStatus()
    }

    private fun loadStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // Calculate how much history we need.
            // Max window for targets is usually 7 days, for fatigue 72 hours.
            // Let's grab 14 days just to be safe and cover all windows.
            val now = Instant.now()
            val lookback = java.time.Duration.ofDays(14)
            val cutoff = now.minus(lookback).toEpochMilli()

            val history = repository.getHistorySince(cutoff)

            val targets = planner.config.targets.map { target ->
                val performed = planner.historyAnalyzer.getPerformed(target.id, target.windowDays, now, history)
                val deficit = max(0.0, target.goal - performed)
                TargetStatus(
                    id = target.id,
                    type = target.type,
                    goal = target.goal.toDouble(),
                    performed = performed,
                    deficit = deficit,
                    windowDays = target.windowDays
                )
            }

            val fatigues = planner.config.fatigueConstraints.flatMap { (kind, constraints) ->
                constraints.map { constraint ->
                    val currentLoad = planner.historyAnalyzer.getAccumulatedFatigue(kind, constraint.windowHours, now, history)
                    FatigueStatus(
                        kind = kind,
                        currentLoad = currentLoad,
                        threshold = constraint.threshold,
                        windowHours = constraint.windowHours,
                        isOk = currentLoad < constraint.threshold,
                        reason = constraint.reason
                    )
                }
            }

            _state.value = StatusState(
                targets = targets,
                fatigues = fatigues,
                isLoading = false
            )
        }
    }
}

class StatusViewModelFactory(
    private val repository: WorkoutRepository,
    private val planner: AdvancedWorkoutPlanner
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatusViewModel(repository, planner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
