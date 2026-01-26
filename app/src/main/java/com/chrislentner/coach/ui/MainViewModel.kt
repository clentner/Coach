package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MainUiState(
    val isLoading: Boolean = true,
    val startDestination: String = "home"
)

class MainViewModel(
    private val repository: ScheduleRepository,
    private val intentDestination: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        if (intentDestination != null) {
            _uiState.value = MainUiState(isLoading = false, startDestination = intentDestination)
        } else {
            checkStartDestination()
        }
    }

    private fun checkStartDestination() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val todaysSchedule = repository.getScheduleByDate(today)

            val destination = if (todaysSchedule == null) {
                "survey"
            } else {
                "home"
            }

            _uiState.value = MainUiState(isLoading = false, startDestination = destination)
        }
    }
}

class MainViewModelFactory(
    private val repository: ScheduleRepository,
    private val intentDestination: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, intentDestination) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
