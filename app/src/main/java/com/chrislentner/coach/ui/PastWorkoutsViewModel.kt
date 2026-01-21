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
            sessions = repository.getSessionsWithSetCounts()
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

    private val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val formatterCurrentYear = SimpleDateFormat("MMM d", Locale.US)
    private val formatterOtherYear = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val calendar = Calendar.getInstance()

    fun formatDate(dateStr: String): String {
        // Input: YYYY-MM-DD
        // Output: "Oct 24" (current year) or "Oct 24, 2023" (other years)
        try {
            val date = parser.parse(dateStr) ?: return dateStr

            calendar.time = date
            val year = calendar.get(Calendar.YEAR)

            calendar.setTimeInMillis(System.currentTimeMillis())
            val currentYear = calendar.get(Calendar.YEAR)

            val formatter = if (year == currentYear) formatterCurrentYear else formatterOtherYear
            return formatter.format(date)
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
