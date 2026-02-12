package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class HistoryAnalyzerTest {

    @Test
    fun `getTargetContributions returns correct contributions`() {
        val now = Instant.now()
        val yesterday = now.minus(1, ChronoUnit.DAYS).toEpochMilli()
        val older = now.minus(10, ChronoUnit.DAYS).toEpochMilli()

        val targetId = "chest_sets"
        val exerciseName = "Bench Press"

        // Mock Config
        val targets = listOf(
            Target(id = targetId, windowDays = 7, type = "sets", goal = 10)
        )
        val prescription = Prescription(
            exercise = exerciseName,
            contributesTo = listOf(Contribution(targetId))
        )
        val block = Block(
            blockName = "Chest Day",
            location = "Gym",
            prescription = listOf(prescription)
        )
        val priorities = mapOf("P1" to PriorityGroup(listOf(block)))

        val config = CoachConfig(
            version = 1,
            targets = targets,
            fatigueConstraints = emptyMap(),
            priorityOrder = emptyList(),
            priorities = priorities,
            selection = SelectionStrategy("default")
        )

        val analyzer = HistoryAnalyzer(config)

        val log1 = WorkoutLogEntry(
            sessionId = 1, exerciseName = exerciseName,
            targetReps = 10, targetDurationSeconds = null, loadDescription = "100lbs",
            actualReps = 10, actualDurationSeconds = null, rpe = 8, notes = null,
            timestamp = yesterday
        )
        val log2 = WorkoutLogEntry(
            sessionId = 2, exerciseName = exerciseName,
            targetReps = 10, targetDurationSeconds = null, loadDescription = "100lbs",
            actualReps = 10, actualDurationSeconds = null, rpe = 8, notes = null,
            timestamp = older
        )
        val logSkipped = WorkoutLogEntry(
            sessionId = 3, exerciseName = exerciseName,
            targetReps = 10, targetDurationSeconds = null, loadDescription = "100lbs",
            actualReps = 0, actualDurationSeconds = null, rpe = null, notes = null,
            skipped = true,
            timestamp = yesterday
        )
        val logOther = WorkoutLogEntry(
            sessionId = 4, exerciseName = "Squat",
            targetReps = 10, targetDurationSeconds = null, loadDescription = "200lbs",
            actualReps = 10, actualDurationSeconds = null, rpe = 8, notes = null,
            timestamp = yesterday
        )

        val history = listOf(log1, log2, logSkipped, logOther)

        val contributions = analyzer.getTargetContributions(targetId, 7, now, history)

        assertEquals(1, contributions.size)
        assertEquals(log1, contributions[0].log)
        assertEquals(1.0, contributions[0].value, 0.001)
    }

    @Test
    fun `getFatigueContributions returns correct load`() {
        val now = Instant.now()
        val recent = now.minus(1, ChronoUnit.HOURS).toEpochMilli()

        val kind = "CNS"
        val exerciseName = "Deadlift"

        // Mock Config
        val prescription = Prescription(
            exercise = exerciseName,
            fatigueLoads = mapOf(kind to 5.0)
        )
        val block = Block(
            blockName = "Leg Day",
            location = "Gym",
            prescription = listOf(prescription)
        )
        val priorities = mapOf("P1" to PriorityGroup(listOf(block)))

        val config = CoachConfig(
            version = 1,
            targets = emptyList(),
            fatigueConstraints = emptyMap(),
            priorityOrder = emptyList(),
            priorities = priorities,
            selection = SelectionStrategy("default")
        )

        val analyzer = HistoryAnalyzer(config)

        val log = WorkoutLogEntry(
            sessionId = 1, exerciseName = exerciseName,
            targetReps = 5, targetDurationSeconds = null, loadDescription = "315lbs",
            actualReps = 5, actualDurationSeconds = null, rpe = 9, notes = null,
            timestamp = recent
        )

        val contributions = analyzer.getFatigueContributions(kind, 24, now, listOf(log))

        assertEquals(1, contributions.size)
        assertEquals(log, contributions[0].log)
        assertEquals(5.0, contributions[0].load, 0.001)
    }

    @Test
    fun `getFatigueContributions ignores skipped logs`() {
        val now = Instant.now()
        val recent = now.minus(1, ChronoUnit.HOURS).toEpochMilli()

        val kind = "CNS"
        val exerciseName = "Deadlift"

        // Mock Config
        val prescription = Prescription(
            exercise = exerciseName,
            fatigueLoads = mapOf(kind to 5.0)
        )
        val block = Block(
            blockName = "Leg Day",
            location = "Gym",
            prescription = listOf(prescription)
        )
        val priorities = mapOf("P1" to PriorityGroup(listOf(block)))

        val config = CoachConfig(
            version = 1,
            targets = emptyList(),
            fatigueConstraints = emptyMap(),
            priorityOrder = emptyList(),
            priorities = priorities,
            selection = SelectionStrategy("default")
        )

        val analyzer = HistoryAnalyzer(config)

        val log = WorkoutLogEntry(
            sessionId = 1, exerciseName = exerciseName,
            targetReps = 5, targetDurationSeconds = null, loadDescription = "315lbs",
            actualReps = 5, actualDurationSeconds = null, rpe = 9, notes = null,
            skipped = true,
            timestamp = recent
        )

        val contributions = analyzer.getFatigueContributions(kind, 24, now, listOf(log))

        assertEquals(0, contributions.size)
    }

    @Test
    fun `HistoryAnalyzer is case-insensitive for exercise names`() {
        val now = Instant.now()
        val recent = now.minus(1, ChronoUnit.HOURS).toEpochMilli()

        val kind = "CNS"
        // Config uses lowercase
        val exerciseName = "deadlift"
        // Log uses Mixed Case
        val logExerciseName = "Deadlift"

        // Mock Config
        val prescription = Prescription(
            exercise = exerciseName,
            fatigueLoads = mapOf(kind to 5.0),
            contributesTo = listOf(Contribution("target_sets"))
        )
        val block = Block(
            blockName = "Leg Day",
            location = "Gym",
            prescription = listOf(prescription)
        )
        val priorities = mapOf("P1" to PriorityGroup(listOf(block)))

        val config = CoachConfig(
            version = 1,
            targets = listOf(Target("target_sets", 7, "sets", 10)),
            fatigueConstraints = emptyMap(),
            priorityOrder = emptyList(),
            priorities = priorities,
            selection = SelectionStrategy("default")
        )

        val analyzer = HistoryAnalyzer(config)

        val log = WorkoutLogEntry(
            sessionId = 1, exerciseName = logExerciseName,
            targetReps = 5, targetDurationSeconds = null, loadDescription = "315lbs",
            actualReps = 5, actualDurationSeconds = null, rpe = 9, notes = null,
            skipped = false,
            timestamp = recent
        )

        // Check Fatigue
        val fatigue = analyzer.getAccumulatedFatigue(kind, 24, now, listOf(log))
        assertEquals(5.0, fatigue, 0.001)

        // Check Deficit (Performed)
        val performed = analyzer.getPerformed("target_sets", 7, now, listOf(log))
        assertEquals(1.0, performed, 0.001)
    }
}
