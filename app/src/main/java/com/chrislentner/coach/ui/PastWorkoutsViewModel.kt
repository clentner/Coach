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

    private val parser = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val formatterCurrentYear = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("MMM d", Locale.US)
    }
    private val formatterOtherYear = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("MMM d, yyyy", Locale.US)
    }
    private val calendar = object : ThreadLocal<Calendar>() {
        override fun initialValue() = Calendar.getInstance()
    }

    fun formatDate(dateStr: String): String {
        // Input: YYYY-MM-DD
        // Output: "Oct 24" (current year) or "Oct 24, 2023" (other years)
        try {
            val sdf = parser.get() ?: return dateStr
            val date = sdf.parse(dateStr) ?: return dateStr

            val cal = calendar.get() ?: Calendar.getInstance()
            cal.time = date
            val year = cal.get(Calendar.YEAR)

            cal.setTimeInMillis(System.currentTimeMillis())
            val currentYear = cal.get(Calendar.YEAR)

            val formatter = if (year == currentYear) formatterCurrentYear.get() else formatterOtherYear.get()
            return formatter?.format(date) ?: dateStr
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
