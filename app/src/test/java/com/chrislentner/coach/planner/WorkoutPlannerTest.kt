package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class WorkoutPlannerTest {

    private fun createLog(exercise: String, daysAgo: Int): WorkoutLogEntry {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        // Set to noon to be safe inside the day
        cal.set(Calendar.HOUR_OF_DAY, 12)
        return WorkoutLogEntry(
            id = 0, sessionId = 0, exerciseName = exercise,
            targetReps = 0, targetDurationSeconds = 0, loadDescription = "",
            actualReps = 0, actualDurationSeconds = 0, rpe = 0, notes = "", skipped = false,
            timestamp = cal.timeInMillis
        )
    }

    @Test
    fun `generatePlan returns Squats when no squats yesterday`() {
        // Empty history
        val plan = WorkoutPlanner.generatePlan(Date(), emptyList())
        assertEquals(3, plan.size)
        assertEquals("Squats", plan[0].exerciseName)
        assertEquals("85 lbs @ 6s/rep", plan[0].loadDescription)
    }

    @Test
    fun `generatePlan returns Squats when squats were 2 days ago`() {
        val history = listOf(createLog("Squats", 2))
        val plan = WorkoutPlanner.generatePlan(Date(), history)
        assertEquals(3, plan.size)
        assertEquals("Squats", plan[0].exerciseName)
    }

    @Test
    fun `generatePlan returns CRACR when squats done yesterday but no CRACR recently`() {
        val history = listOf(createLog("Squats", 1))
        val plan = WorkoutPlanner.generatePlan(Date(), history)
        assertEquals(2, plan.size)
        assertEquals("Hamstring CRACR", plan[0].exerciseName)
    }

    @Test
    fun `generatePlan returns Overhead Press when squats done yesterday and CRACR done recently`() {
        val history = listOf(
            createLog("Squats", 1),
            createLog("Hamstring CRACR", 2)
        )
        val plan = WorkoutPlanner.generatePlan(Date(), history)
        assertEquals(3, plan.size)
        assertEquals("Overhead Press", plan[0].exerciseName)
        assertEquals("45 lbs", plan[0].loadDescription)
    }
}
