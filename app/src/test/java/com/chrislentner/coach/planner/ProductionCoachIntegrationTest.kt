package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Date

class ProductionCoachIntegrationTest {

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
             // Try relative to module if running from module root
             file = File("src/main/assets/coach.yaml")
        }
        if (!file.exists()) {
            throw RuntimeException("Could not find coach.yaml. CWD: " + System.getProperty("user.dir"))
        }

        config = mapper.readValue(file, CoachConfig::class.java)

        historyAnalyzer = HistoryAnalyzer(config)
        progressionEngine = ProgressionEngine(historyAnalyzer)
        planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
    }

    @Test
    fun `first scheduled exercise should be from top priority patellar_tendon`() {
        val today = Date()
        val history = emptyList<WorkoutLogEntry>()
        // 60 minutes, gym
        val schedule = ScheduleEntry("2024-01-01", today.time, 60, "gym")

        val plan = planner.generatePlan(today, history, schedule)

        if (plan.isEmpty()) {
            fail("Plan should not be empty")
        }

        val firstExercise = plan.first()

        // Expected exercises from 'patellar_tendon' priority group
        val expectedExercises = setOf("squat", "single_leg_press", "stairmaster")

        assertTrue(
            "Expected first exercise to be one of $expectedExercises, but was ${firstExercise.exerciseName}",
            expectedExercises.contains(firstExercise.exerciseName)
        )
    }
}
