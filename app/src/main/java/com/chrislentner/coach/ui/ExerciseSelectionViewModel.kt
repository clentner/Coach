package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseSelectionViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    init {
        viewModelScope.launch {
            val recents = repository.getRecentExerciseNames(20)
            val defaults = com.chrislentner.coach.planner.WorkoutPlanner.DEFAULT_EXERCISES
            _suggestions.value = (recents + defaults).distinct()
        }
    }
}

class ExerciseSelectionViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseSelectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
