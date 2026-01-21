package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SuggestedScheduleViewModel(
    private val workoutRepository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: AdvancedWorkoutPlanner?
) : ViewModel() {

    var dayPlans by mutableStateOf<List<SuggestedDayPlan>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        buildSuggestedPlan()
    }

    fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            val year = cal.get(Calendar.YEAR)

            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)

            val pattern = if (year == currentYear) "MMM d" else "MMM d, yyyy"
            SimpleDateFormat(pattern, Locale.US).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getPlanForDay(index: Int): SuggestedDayPlan? {
        return dayPlans.getOrNull(index)
    }

    private fun buildSuggestedPlan() {
        viewModelScope.launch {
            if (planner == null) {
                errorMessage = "Planner unavailable."
                isLoading = false
                return@launch
            }

            val now = Date()
            val historyWindowDays = planner.getHistoryWindowDays()
            val historyStart = now.time - TimeUnit.DAYS.toMillis(historyWindowDays.toLong())
            val history = workoutRepository.getHistorySince(historyStart).toMutableList()
            val fallbackSchedule = scheduleRepository.getLastSchedule()
            val priorityTargets = planner.getTargetPriorityOrder()

            val plans = mutableListOf<SuggestedDayPlan>()

            for (dayOffset in 0 until 7) {
                val dayDate = getStartOfDay(now, dayOffset)
                val dateStr = formatDateKey(dayDate)
                val baseSchedule = scheduleRepository.getScheduleByDate(dateStr)
                    ?: fallbackSchedule
                    ?: defaultSchedule(dateStr, dayDate)

                val scheduleForDay = baseSchedule.copy(
                    date = dateStr,
                    timeInMillis = alignTime(baseSchedule.timeInMillis, dayDate)
                )

                val homeSchedule = scheduleForDay.copy(location = "Home")
                val gymSchedule = scheduleForDay.copy(location = "Gym")

                val homePlan = planner.generatePlanResult(dayDate, history, homeSchedule)
                val gymPlan = planner.generatePlanResult(dayDate, history, gymSchedule)

                val chosenPlan = choosePlan(homePlan, gymPlan, priorityTargets)
                val chosenSchedule = if (chosenPlan === homePlan) homeSchedule else gymSchedule

                history.addAll(chosenPlan.dummyLogs)

                val summary = SessionSummary(
                    id = dayOffset.toLong(),
                    date = dateStr,
                    location = chosenSchedule.location,
                    setCount = chosenPlan.steps.size
                )
                plans.add(SuggestedDayPlan(summary, chosenPlan.dummyLogs))
            }

            dayPlans = plans
            isLoading = false
        }
    }

    private fun choosePlan(
        homePlan: AdvancedWorkoutPlanner.PlanResult,
        gymPlan: AdvancedWorkoutPlanner.PlanResult,
        priorityTargets: List<String>
    ): AdvancedWorkoutPlanner.PlanResult {
        priorityTargets.forEach { target ->
            val homeDeficit = homePlan.deficits[target] ?: 0.0
            val gymDeficit = gymPlan.deficits[target] ?: 0.0
            if (homeDeficit < gymDeficit) return homePlan
            if (gymDeficit < homeDeficit) return gymPlan
        }
        return homePlan
    }

    private fun defaultSchedule(dateStr: String, date: Date): ScheduleEntry {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return ScheduleEntry(
            date = dateStr,
            timeInMillis = calendar.timeInMillis,
            durationMinutes = 45,
            location = "Home"
        )
    }

    private fun getStartOfDay(date: Date, offsetDays: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Date(cal.timeInMillis)
    }

    private fun formatDateKey(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(date)
    }

    private fun alignTime(baseTimeMillis: Long, date: Date): Long {
        val baseCal = Calendar.getInstance()
        baseCal.timeInMillis = baseTimeMillis
        val targetCal = Calendar.getInstance()
        targetCal.time = date
        targetCal.set(Calendar.HOUR_OF_DAY, baseCal.get(Calendar.HOUR_OF_DAY))
        targetCal.set(Calendar.MINUTE, baseCal.get(Calendar.MINUTE))
        targetCal.set(Calendar.SECOND, 0)
        targetCal.set(Calendar.MILLISECOND, 0)
        return targetCal.timeInMillis
    }
}

data class SuggestedDayPlan(
    val summary: SessionSummary,
    val logs: List<WorkoutLogEntry>
)

class SuggestedScheduleViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: AdvancedWorkoutPlanner?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuggestedScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuggestedScheduleViewModel(workoutRepository, scheduleRepository, planner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
