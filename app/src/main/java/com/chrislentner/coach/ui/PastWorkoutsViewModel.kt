package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PastWorkoutsViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    var sessions by mutableStateOf<List<SessionSummary>>(emptyList())
        private set

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            repository.getSessionsWithSetCountsFlow().collect {
                sessions = it
            }
        }
    }

    fun createSession(millis: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val instant = Instant.ofEpochMilli(millis)
            val date = instant.atZone(ZoneId.of("UTC")).toLocalDate()
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val session = repository.getOrCreateSession(dateStr, millis)
            onCreated(session.id)
        }
    }

    fun formatDate(dateStr: String): String {
        // Input: YYYY-MM-DD
        // Output: "Oct 24" (current year) or "Oct 24, 2023" (other years)
        try {
            val date = LocalDate.parse(dateStr)
            val currentYear = LocalDate.now().year

            val pattern = if (date.year == currentYear) "MMM d" else "MMM d, yyyy"
            return date.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        } catch (e: Exception) {
            return dateStr
        }
    }
}

class PastWorkoutsViewModelFactory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PastWorkoutsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PastWorkoutsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
