package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class HistoryAnalyzerLibraryTest {

    @Test
    fun `reproduce library exercise ignored bug`() {
        val now = Instant.now()
        val recent = now.minus(1, ChronoUnit.HOURS).toEpochMilli()

        val exerciseName = "nordic_ski"
        val kind = "knee"

        // Mock Config
        val prescription = Prescription(
            exercise = exerciseName,
            fatigueLoads = mapOf(kind to 0.5)
        )
        val block = Block(
            blockName = "nordic_ski_1_mile",
            location = "anywhere",
            prescription = listOf(prescription)
        )
        // Put block in LIBRARY only
        val library = mapOf("nordic_ski" to PriorityGroup(listOf(block)))

        val config = CoachConfig(
            version = 1,
            targets = emptyList(),
            fatigueConstraints = emptyMap(),
            priorityOrder = emptyList(),
            priorities = emptyMap(), // Empty priorities
            library = library,
            selection = SelectionStrategy("default")
        )

        val analyzer = HistoryAnalyzer(config)

        val log = WorkoutLogEntry(
            sessionId = 1, exerciseName = exerciseName,
            targetReps = null, targetDurationSeconds = null, loadDescription = "distance",
            actualReps = null, actualDurationSeconds = null, rpe = null, notes = null,
            skipped = false,
            timestamp = recent
        )

        // Check Fatigue
        val fatigue = analyzer.getAccumulatedFatigue(kind, 24, now, listOf(log))
        assertEquals("Library exercise should contribute to fatigue", 0.5, fatigue, 0.001)
    }
}
