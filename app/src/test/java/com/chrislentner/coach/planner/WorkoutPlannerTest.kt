package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutPlannerTest {

    private fun createLog(exercise: String, daysAgo: Int): WorkoutLogEntry {
        val cal = ZonedDateTime.now(ZoneId.systemDefault())
            .minusDays(daysAgo.toLong())
            .withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return WorkoutLogEntry(
            id = 0, sessionId = 0, exerciseName = exercise,
            targetReps = 0, targetDurationSeconds = 0, loadDescription = "",
            tempo = null,
            actualReps = 0, actualDurationSeconds = 0, rpe = 0, notes = "", skipped = false,
            timestamp = cal.toInstant().toEpochMilli()
        )
    }

    @Test
    fun `generatePlan returns Squats when no squats yesterday`() {
        // Empty history
        val plan = WorkoutPlanner.generatePlan(Instant.now(), emptyList())
        assertEquals(3, plan.size)
        assertEquals("Squats", plan[0].exerciseName)
        assertEquals("85 lbs", plan[0].loadDescription)
        assertEquals("3030", plan[0].tempo)
    }

    @Test
    fun `generatePlan returns Squats when squats were 2 days ago`() {
        val history = listOf(createLog("Squats", 2))
        val plan = WorkoutPlanner.generatePlan(Instant.now(), history)
        assertEquals(3, plan.size)
        assertEquals("Squats", plan[0].exerciseName)
    }

    @Test
    fun `generatePlan returns CRACR when squats done yesterday but no CRACR recently`() {
        val history = listOf(createLog("Squats", 1))
        val plan = WorkoutPlanner.generatePlan(Instant.now(), history)
        assertEquals(2, plan.size)
        assertEquals("Hamstring CRACR", plan[0].exerciseName)
    }

    @Test
    fun `generatePlan returns Overhead Press when squats done yesterday and CRACR done recently`() {
        val history = listOf(
            createLog("Squats", 1),
            createLog("Hamstring CRACR", 2)
        )
        val plan = WorkoutPlanner.generatePlan(Instant.now(), history)
        assertEquals(3, plan.size)
        assertEquals("Overhead Press", plan[0].exerciseName)
        assertEquals("45 lbs", plan[0].loadDescription)
    }
}
