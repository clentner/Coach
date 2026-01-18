package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class FatigueNormalizationTest {

    @Test
    fun `fatigue load is normalized by sets`() {
        val yaml = """
            version: 3
            targets: []
            fatigue_constraints: {}
            priority_order: [p1]
            priorities:
              p1:
                blocks:
                  - block_name: heavy_squat
                    size_minutes: [30]
                    location: gym
                    tags: []
                    contributes_to: []
                    prescription:
                      - exercise: squat
                        sets: 4
                        fatigue_loads:
                          knee: 1.0
            selection:
              strategy: greedy_strict
        """.trimIndent()

        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val config = mapper.readValue(yaml, CoachConfig::class.java)

        val historyAnalyzer = HistoryAnalyzer(config)

        val today = Date()
        val log = WorkoutLogEntry(
            sessionId = 1,
            exerciseName = "squat",
            targetReps = 5,
            targetDurationSeconds = null,
            loadDescription = "100",
            actualReps = 5,
            actualDurationSeconds = 60,
            rpe = null,
            notes = "",
            timestamp = today.time,
            skipped = false,
            tempo = null
        )

        // 1 log entry (1 set).
        // Config: 4 sets, 1.0 load.
        // Expected per set: 0.25.

        val load = historyAnalyzer.getAccumulatedFatigue("knee", 24, today, listOf(log))

        assertEquals(0.25, load, 0.001)
    }
}
