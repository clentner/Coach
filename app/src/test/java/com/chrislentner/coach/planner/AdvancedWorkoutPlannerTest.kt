package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class AdvancedWorkoutPlannerTest {

    private lateinit var config: CoachConfig
    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var planner: AdvancedWorkoutPlanner

    @Before
    fun setup() {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val file = java.io.File("src/test/resources/test_coach.yaml")
        val inputStream = if (file.exists()) {
             java.io.FileInputStream(file)
        } else {
             java.io.FileInputStream("app/src/test/resources/test_coach.yaml")
        }

        config = mapper.readValue(inputStream, CoachConfig::class.java)

        historyAnalyzer = HistoryAnalyzer(config)
        progressionEngine = ProgressionEngine(historyAnalyzer)
        planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
    }

    @Test
    fun `generatePlan selects BlockA then BlockB due to fatigue`() {
        val today = Date()
        val history = emptyList<WorkoutLogEntry>()
        val schedule = ScheduleEntry("2023-10-27", today.time, 30, "anywhere") // 30 mins available

        val plan = planner.generatePlan(today, history, schedule)

        // Expected:
        // 1. BlockA (10 mins, 0.6 fatigue). Deficit T1 (Sets 10) -> 8.
        // 2. Try BlockA again? Fatigue 0.6+0.6=1.2 > 1.0. Rejected.
        // 3. BlockB (10 mins, 0 fatigue). Deficit T2 (Mins 30) -> 20.
        // 4. Try BlockB again? Deficit T2 (20). Fits. OK.
        // 5. BlockB (10 mins). Deficit T2 -> 10.
        // Total time: 10 + 10 + 10 = 30.

        // Steps count:
        // BlockA (2 sets) -> 2 steps.
        // BlockB (1 set) -> 1 step.
        // BlockB (1 set) -> 1 step.
        // Total 4 steps.
        // UPDATE: Due to unique exercise constraint, BlockB (ex_b) cannot be selected again.
        // So we stop after one BlockB.
        // Total time: 10 + 10 = 20.
        // Steps: BlockA(2) + BlockB(1) = 3.

        assertEquals(3, plan.size)
        assertEquals("ex_a", plan[0].exerciseName)
        assertEquals("ex_a", plan[1].exerciseName)
        assertEquals("ex_b", plan[2].exerciseName)
    }

    @Test
    fun `generatePlan does not repeat exercises in the same session`() {
        val today = Date()
        val history = emptyList<WorkoutLogEntry>()
        // Schedule enough time for Block A (10) + Block B (10) + Block B (10) = 30 mins
        // Block A limits on fatigue, so it might appear once.
        // Block B has no fatigue limits, so without uniqueness check, it would appear multiple times to fill the 30 mins.
        val schedule = ScheduleEntry("2023-10-27", today.time, 30, "anywhere")

        val plan = planner.generatePlan(today, history, schedule)

        // Block A uses "ex_a". Block B uses "ex_b".
        // With uniqueness enforcement:
        // 1. Block A (ex_a) selected.
        // 2. Block B (ex_b) selected.
        // 3. Block B (ex_b) REJECTED.

        // Verify we have ex_a and ex_b
        val exercises = plan.map { it.exerciseName }
        assertTrue(exercises.contains("ex_a"))
        assertTrue(exercises.contains("ex_b"))

        // Count occurrences
        val exBCount = exercises.count { it == "ex_b" }
        assertEquals("Exercise 'ex_b' should strictly appear once", 1, exBCount)
    }

    @Test
    fun `progression increments load after 1 session`() {
        val today = Date()
        val oneDayAgo = Date(today.time - 25 * 3600 * 1000) // 25 hours ago to avoid fatigue window

        // History contains 1 session of BlockA with 100 lbs
        val log1 = WorkoutLogEntry(
            sessionId = 1,
            exerciseName = "ex_a",
            targetReps = 5,
            targetDurationSeconds = null,
            loadDescription = "100 lbs",
            actualReps = 5,
            actualDurationSeconds = null,
            rpe = null,
            notes = "",
            timestamp = oneDayAgo.time,
            skipped = false
        )
        // Log needs 2 entries because BlockA is 2 sets?
        // Heuristic: >= 50%. 1 set of 2 is 50%. So 1 log is enough.

        val history = listOf(log1)

        val schedule = ScheduleEntry("2023-10-27", today.time, 15, "anywhere")

        val plan = planner.generatePlan(today, history, schedule)

        // BlockA progression: every 1 session, increment 10 lbs.
        // Start 100. History has 100. Next should be 110.

        assertEquals(2, plan.size) // 1 BlockA fits (10 mins).
        assertEquals("110 lbs", plan[0].loadDescription)
    }
}
