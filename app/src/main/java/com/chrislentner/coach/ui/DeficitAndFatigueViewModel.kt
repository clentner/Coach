package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.HistoryAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class DeficitAndFatigueState(
    val deficits: Map<String, Double> = emptyMap(),
    val fatigue: Map<String, Double> = emptyMap()
)

class DeficitAndFatigueViewModel(
    private val historyAnalyzer: HistoryAnalyzer,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeficitAndFatigueState())
    val uiState: StateFlow<DeficitAndFatigueState> = _uiState

    init {
        viewModelScope.launch {
            val history = workoutRepository.getHistorySince(0)
            val now = Date()
            val deficits = historyAnalyzer.getDeficits(history, now)
            val fatigue = historyAnalyzer.getAccumulatedFatigue(history, now)
            _uiState.value = DeficitAndFatigueState(
                deficits = deficits,
                fatigue = fatigue
            )
        }
    }
}

class DeficitAndFatigueViewModelFactory(
    private val historyAnalyzer: HistoryAnalyzer,
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeficitAndFatigueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeficitAndFatigueViewModel(historyAnalyzer, workoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
