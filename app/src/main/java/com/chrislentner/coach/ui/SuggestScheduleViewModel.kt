package com.chrislentner.coach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.Plan
import com.chrislentner.coach.planner.WorkoutStep
import com.chrislentner.coach.planner.model.CoachConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SuggestedDayPlan(
    val date: Date,
    val isRestDay: Boolean,
    val location: String?,
    val steps: List<WorkoutStep>
)

class SuggestScheduleViewModel(
    private val repository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: AdvancedWorkoutPlanner,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    var suggestedPlans by mutableStateOf<List<SuggestedDayPlan>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        generateSchedule()
    }

    private fun generateSchedule() {
        viewModelScope.launch {
            withContext(dispatcher) {
                // Fetch History
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -60) // Safe margin of 60 days
                val history = repository.getHistorySince(cal.timeInMillis).toMutableList()

                val workingHistory = ArrayList(history)
                val plans = mutableListOf<SuggestedDayPlan>()

                val currentCal = Calendar.getInstance()
                // Do not reset time here; start from "Now"

                for (i in 0 until 7) {
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentCal.time)

                    val schedule = scheduleRepository.getScheduleByDate(dateStr)
                        ?: ScheduleEntry(dateStr, isRestDay = false, timeInMillis = null, durationMinutes = 60, location = null)

                    // Adjust time if scheduled
                    if (schedule.timeInMillis != null) {
                        val scheduleCal = Calendar.getInstance()
                        scheduleCal.timeInMillis = schedule.timeInMillis

                        currentCal.set(Calendar.HOUR_OF_DAY, scheduleCal.get(Calendar.HOUR_OF_DAY))
                        currentCal.set(Calendar.MINUTE, scheduleCal.get(Calendar.MINUTE))
                    }

                    val currentDate = currentCal.time

                    if (schedule.isRestDay) {
                        plans.add(SuggestedDayPlan(currentDate, true, null, emptyList()))
                    } else {
                        // Location logic
                        val chosenPlan: Plan
                        val chosenLocation: String

                        if (schedule.location != null) {
                            chosenLocation = schedule.location
                            chosenPlan = planner.generatePlan(currentDate, workingHistory, schedule)
                        } else {
                            // Compare Home vs Gym
                            val homeSchedule = schedule.copy(location = "Home")
                            val gymSchedule = schedule.copy(location = "Gym")

                            val homePlan = planner.generatePlan(currentDate, workingHistory, homeSchedule)
                            val gymPlan = planner.generatePlan(currentDate, workingHistory, gymSchedule)

                            val winner = comparePlans(homePlan, gymPlan, planner.config)
                            chosenPlan = winner
                            chosenLocation = if (winner === homePlan) "Home" else "Gym"
                        }

                        workingHistory.addAll(chosenPlan.logs)
                        plans.add(SuggestedDayPlan(currentDate, false, chosenLocation, chosenPlan.steps))
                    }

                    // Advance to next day
                    currentCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                withContext(Dispatchers.Main) {
                    suggestedPlans = plans
                    isLoading = false
                }
            }
        }
    }

    private fun comparePlans(home: Plan, gym: Plan, config: CoachConfig): Plan {
        for (groupName in config.priorityOrder) {
            val blocksInGroup = config.priorities[groupName]?.blocks?.map { it.blockName }?.toSet() ?: emptySet()

            val homeScore = home.blocks.filter { it.blockName in blocksInGroup }.sumOf { it.reduction }
            val gymScore = gym.blocks.filter { it.blockName in blocksInGroup }.sumOf { it.reduction }

            if (homeScore > gymScore + 0.001) return home
            if (gymScore > homeScore + 0.001) return gym
        }
        return home // Default
    }
}

class SuggestScheduleViewModelFactory(
    private val repository: WorkoutRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: AdvancedWorkoutPlanner
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuggestScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuggestScheduleViewModel(repository, scheduleRepository, planner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
