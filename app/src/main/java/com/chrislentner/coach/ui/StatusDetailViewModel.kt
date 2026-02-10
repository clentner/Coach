package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.FatigueContribution
import com.chrislentner.coach.planner.TargetContribution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed class StatusDetailType {
    data class Target(val targetId: String) : StatusDetailType()
    data class Fatigue(val kind: String, val windowHours: Int) : StatusDetailType()
}

data class StatusDetailState(
    val title: String = "",
    val type: String = "", // "sets", "minutes", or "load"
    val targetContributions: List<TargetContribution> = emptyList(),
    val fatigueContributions: List<FatigueContribution> = emptyList(),
    val isLoading: Boolean = true
)

class StatusDetailViewModel(
    private val repository: WorkoutRepository,
    private val planner: AdvancedWorkoutPlanner,
    private val detailType: StatusDetailType
) : ViewModel() {

    private val _state = MutableStateFlow(StatusDetailState())
    val state: StateFlow<StatusDetailState> = _state

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val now = Instant.now()
            // Decide lookback based on type
            val lookbackDays = when (detailType) {
                is StatusDetailType.Target -> {
                     val target = planner.config.targets.find { it.id == detailType.targetId }
                     target?.windowDays ?: 14
                }
                is StatusDetailType.Fatigue -> {
                    // Window hours to days, round up + buffer
                    (detailType.windowHours / 24) + 2
                }
            }

            val lookbackDuration = java.time.Duration.ofDays(lookbackDays.toLong())
            val cutoff = now.minus(lookbackDuration).toEpochMilli()
            val history = repository.getHistorySince(cutoff)

            when (detailType) {
                is StatusDetailType.Target -> {
                    val targetConfig = planner.config.targets.find { it.id == detailType.targetId }
                    if (targetConfig != null) {
                        val contributions = planner.historyAnalyzer.getTargetContributions(
                            detailType.targetId,
                            targetConfig.windowDays,
                            now,
                            history
                        ).sortedByDescending { it.log.timestamp }

                        _state.value = StatusDetailState(
                            title = formatTargetName(targetConfig.id),
                            type = targetConfig.type,
                            targetContributions = contributions,
                            isLoading = false
                        )
                    } else {
                         _state.value = StatusDetailState(isLoading = false)
                    }
                }
                is StatusDetailType.Fatigue -> {
                    val contributions = planner.historyAnalyzer.getFatigueContributions(
                        detailType.kind,
                        detailType.windowHours,
                        now,
                        history
                    ).sortedByDescending { it.log.timestamp }

                    _state.value = StatusDetailState(
                        title = "${detailType.kind.replace("_", " ").capitalizeWords()} Fatigue",
                        type = "load",
                        fatigueContributions = contributions,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun formatTargetName(id: String): String {
        return id.replace("_", " ").capitalizeWords()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

class StatusDetailViewModelFactory(
    private val repository: WorkoutRepository,
    private val planner: AdvancedWorkoutPlanner,
    private val detailType: StatusDetailType
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatusDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatusDetailViewModel(repository, planner, detailType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
