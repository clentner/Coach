package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PlannerDayUiModel(
    val date: String, // YYYY-MM-DD
    val dayOfWeek: String, // e.g. "Monday"
    val displayDate: String, // e.g. "Oct 24"
    val isRestDay: Boolean,
    val details: String? // e.g. "8:00 AM - Gym" or null
)

class WeeklyPlannerViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    var days by mutableStateOf<List<PlannerDayUiModel>>(emptyList())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val dayOfWeekFormat = DateTimeFormatter.ofPattern("EEEE", Locale.US)
            val displayDateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

            val startLocalDate = LocalDate.now(zoneId)
            val startDate = startLocalDate.format(dateFormat)

            // Calculate end date (6 days from now)
            val endDate = startLocalDate.plusDays(6).format(dateFormat)

            val entries = repository.getScheduleBetweenDates(startDate, endDate)
            val entriesMap = entries.associateBy { it.date }

            val newDays = mutableListOf<PlannerDayUiModel>()

            var loopDate = startLocalDate

            for (i in 0..6) {
                val dateStr = loopDate.format(dateFormat)
                val entry = entriesMap[dateStr]

                val isRestDay = entry?.isRestDay == true

                val details = if (entry != null && !isRestDay && entry.timeInMillis != null) {
                    val timeStr = Instant.ofEpochMilli(entry.timeInMillis)
                        .atZone(zoneId)
                        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
                    "$timeStr - ${entry.location ?: "Unknown"}"
                } else {
                    null
                }

                newDays.add(
                    PlannerDayUiModel(
                        date = dateStr,
                        dayOfWeek = dayOfWeekFormat.format(loopDate),
                        displayDate = displayDateFormat.format(loopDate),
                        isRestDay = isRestDay,
                        details = details
                    )
                )

                loopDate = loopDate.plusDays(1)
            }

            days = newDays
        }
    }

    fun toggleRestDay(day: PlannerDayUiModel) {
        viewModelScope.launch {
            val currentEntry = repository.getScheduleByDate(day.date)

            val newIsRestDay = !day.isRestDay

            val newEntry = if (currentEntry != null) {
                currentEntry.copy(isRestDay = newIsRestDay)
            } else {
                // If creating a new entry, strictly mostly nulls
                ScheduleEntry(
                    date = day.date,
                    timeInMillis = null,
                    durationMinutes = null,
                    location = null,
                    isRestDay = newIsRestDay
                )
            }

            repository.saveSchedule(newEntry)
            refresh() // Reload to refresh UI
        }
    }
}

class WeeklyPlannerViewModelFactory(
    private val repository: ScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeeklyPlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeeklyPlannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
