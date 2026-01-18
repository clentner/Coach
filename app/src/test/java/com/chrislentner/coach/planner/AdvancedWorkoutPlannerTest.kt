package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertEquals
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

        // Updated logic with prior_load_lt:
        // Threshold 1.0. BlockA load 0.4 (2 sets * 0.4? No, assume prescription fatigue_loads is per prescription block or per set?
        // Logic: for each log -> load. BlockA has 2 sets -> 2 logs. Each log maps to 0.4 load. Total 0.8.
        // 1. BlockA (load 0.8). Prior load 0. Accepted.
        // 2. BlockA (load 0.8). Prior load 0.8. 0.8 < 1.0. Accepted.
        // 3. BlockA (load 0.8). Prior load 1.6. 1.6 >= 1.0. Rejected.
        // Wait, 1st BlockA contributes 0.8 load to accumulated.
        // So before 2nd BlockA: Accumulated = 0.8.
        // 0.8 < 1.0. Accepted.
        // Before 3rd BlockA: Accumulated = 0.8 + 0.8 = 1.6.
        // 1.6 >= 1.0. Rejected.

        // So result:
        // BlockA (2 steps)
        // BlockA (2 steps)
        // Remaining time: 10 mins.
        // BlockB (10 mins, load 0). Accepted.

        // Total steps: 2 + 2 + 1 = 5 steps.

        assertEquals(5, plan.size)
        assertEquals("ex_a", plan[0].exerciseName)
        assertEquals("ex_a", plan[2].exerciseName)
        assertEquals("ex_b", plan[4].exerciseName)
    }

    @Test
    fun `generatePlan respects prior_load_lt by excluding candidate load`() {
        val yaml = """
            version: 3
            targets:
              - id: t1
                window_days: 7
                type: sets
                goal: 10
            fatigue_constraints:
              knee:
                - kind: prior_load_lt
                  window_hours: 24
                  threshold: 0.8
                  applies_to_blocks_with_tag: [knee_heavy]
                  reason: "Limit knee load"
            priority_order:
              - p1
            priorities:
              p1:
                blocks:
                  - block_name: big_block
                    size_minutes: [10]
                    location: anywhere
                    tags: [knee_heavy]
                    contributes_to:
                      - { target: t1 }
                    prescription:
                      - exercise: big_lift
                        sets: 1
                        fatigue_loads:
                          knee: 1.0
            selection:
              strategy: greedy_strict
        """.trimIndent()

        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val config = mapper.readValue(yaml, CoachConfig::class.java)

        val ha = HistoryAnalyzer(config)
        val pe = ProgressionEngine(ha)
        val p = AdvancedWorkoutPlanner(config, ha, pe)

        val today = Date()
        val history = emptyList<WorkoutLogEntry>()
        val schedule = ScheduleEntry("2023-10-27", today.time, 10, "anywhere")

        val plan = p.generatePlan(today, history, schedule)

        // Prior load is 0. Candidate load is 1.0. Threshold is 0.8.
        // Old logic: 1.0 >= 0.8 -> Rejected.
        // New logic: 0.0 < 0.8 -> Accepted.
        assertEquals(1, plan.size)
        assertEquals("big_lift", plan[0].exerciseName)
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
