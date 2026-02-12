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
    fun `past workout timestamp should use local midnight not UTC midnight`() {
        // Bug: DatePicker returns UTC midnight for the selected date, and this was used
        // as the log entry timestamp. For users in western timezones (e.g. EST = UTC-5),
        // Feb 9 00:00 UTC is actually Feb 8 7PM local. When checking status late on
        // Feb 11, the 72h cutoff (Feb 9 01:30 UTC) is AFTER the log timestamp,
        // so the log falls outside the 72h window despite being "2 days ago."
        //
        // Fix: EditExerciseViewModel now derives the timestamp from the session's date
        // string in the user's local timezone, so Feb 9 becomes Feb 9 00:00 local
        // (= Feb 9 05:00 UTC for EST), which stays within the 72h window.

        val est = java.time.ZoneId.of("America/New_York")

        // After fix: timestamp is local midnight = Feb 9 00:00 EST = Feb 9 05:00 UTC
        val feb9LocalMidnight = java.time.LocalDate.of(2026, 2, 9)
            .atStartOfDay(est)
            .toInstant()
            .toEpochMilli()

        // User checks status on Feb 11 at 8:30 PM EST = Feb 12 01:30 UTC
        val now = java.time.ZonedDateTime.of(2026, 2, 11, 20, 30, 0, 0, est)
            .toInstant()

        val targets = listOf(
            Target(id = "patellar_hsr_squat_sets", windowDays = 7, type = "sets", goal = 4)
        )
        val prescription = Prescription(
            exercise = "squat",
            fatigueLoads = mapOf("cns" to 1.0, "knee" to 1.0),
            contributesTo = listOf(Contribution("patellar_hsr_squat_sets"))
        )
        val block = Block(
            blockName = "hsr_squat",
            location = "gym",
            tags = listOf("compound_heavy"),
            prescription = listOf(prescription)
        )
        val config = CoachConfig(
            version = 1,
            targets = targets,
            fatigueConstraints = mapOf(
                "cns" to listOf(
                    FatigueConstraint(
                        kind = "prior_load_lt",
                        windowHours = 72,
                        threshold = 2.0,
                        appliesToBlocksWithTag = listOf("compound_heavy"),
                        reason = "Heavy compounds only if CNS load (72h) < 2.0"
                    )
                )
            ),
            priorityOrder = listOf("patellar_tendon"),
            priorities = mapOf("patellar_tendon" to PriorityGroup(listOf(block))),
            selection = SelectionStrategy("greedy_strict")
        )

        val analyzer = HistoryAnalyzer(config)

        val log1 = WorkoutLogEntry(
            sessionId = 1, exerciseName = "squat",
            targetReps = 8, targetDurationSeconds = null, loadDescription = "85 lbs",
            actualReps = 8, actualDurationSeconds = null, rpe = null,
            notes = "Manual Entry", timestamp = feb9LocalMidnight
        )
        val log2 = WorkoutLogEntry(
            sessionId = 1, exerciseName = "squat",
            targetReps = 8, targetDurationSeconds = null, loadDescription = "85 lbs",
            actualReps = 8, actualDurationSeconds = null, rpe = null,
            notes = "Manual Entry", timestamp = feb9LocalMidnight
        )
        val history = listOf(log1, log2)

        val deficit = analyzer.getDeficit("patellar_hsr_squat_sets", 7, now, history)
        assertEquals("Target should count both sets", 2.0, deficit, 0.001)

        val cnsLoad = analyzer.getAccumulatedFatigue("cns", 72, now, history)
        assertEquals("CNS load should be 2.0", 2.0, cnsLoad, 0.001)
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
}
