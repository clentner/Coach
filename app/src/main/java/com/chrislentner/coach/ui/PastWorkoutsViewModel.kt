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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
            val date = Date(millis)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dateStr = formatter.format(date)

            val session = repository.getOrCreateSession(dateStr, millis)
            onCreated(session.id)
        }
    }

    fun formatDate(dateStr: String): String {
        // Input: YYYY-MM-DD
        // Output: "Oct 24" (current year) or "Oct 24, 2023" (other years)
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            val year = cal.get(Calendar.YEAR)

            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)

            val pattern = if (year == currentYear) "MMM d" else "MMM d, yyyy"
            return SimpleDateFormat(pattern, Locale.US).format(date)
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
