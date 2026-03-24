package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class MobilityFatigueTest {

    private lateinit var config: CoachConfig
    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var planner: AdvancedWorkoutPlanner

    @Before
    fun setup() {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        var file = File("app/src/main/assets/coach.yaml")
        if (!file.exists()) {
             file = File("src/main/assets/coach.yaml")
        }

        config = mapper.readValue(file, CoachConfig::class.java)
        historyAnalyzer = HistoryAnalyzer(config)
        progressionEngine = ProgressionEngine(historyAnalyzer)
        planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
    }

    @Test
    fun `mobility block is not scheduled if pike_lift_cracr was performed within 48h`() {
        val today = Instant.now()
        // 24 hours ago
        val yesterday = today.minus(24, ChronoUnit.HOURS)

        val history = listOf(
            WorkoutLogEntry(
                id = 1L,
                sessionId = 1L,
                exerciseName = "pike_lift_cracr",
                targetReps = 5,
                targetDurationSeconds = null,
                loadDescription = "bodyweight",
                tempo = null,
                actualReps = 5,
                actualDurationSeconds = null,
                rpe = null,
                notes = "",
                timestamp = yesterday.toEpochMilli()
            )
        )

        // Give plenty of time and 'Anywhere' location so mobility could be scheduled
        val schedule = ScheduleEntry("2024-01-02", today.toEpochMilli(), 120, "Anywhere")

        val planResult = planner.generatePlan(today, history, schedule)
        val plannedExercises = planResult.steps.map { it.exerciseName }.toSet()

        assertFalse(
            "Expected pike_lift_cracr to be blocked due to fatigue, but it was scheduled",
            plannedExercises.contains("pike_lift_cracr")
        )
    }
}
