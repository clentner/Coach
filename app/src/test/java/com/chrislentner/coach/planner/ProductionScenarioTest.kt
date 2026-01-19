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
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class ProductionScenarioTest {

    private lateinit var config: CoachConfig
    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var planner: AdvancedWorkoutPlanner

    @Before
    fun setup() {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // Try to locate the production config file
        val possiblePaths = listOf(
            "app/src/main/assets/coach.yaml",
            "src/main/assets/coach.yaml",
            "../app/src/main/assets/coach.yaml"
        )

        val file = possiblePaths.map { File(it) }.firstOrNull { it.exists() }
            ?: throw RuntimeException("Could not find coach.yaml in ${possiblePaths.map { File(it).absolutePath }}")

        config = mapper.readValue(file, CoachConfig::class.java)

        historyAnalyzer = HistoryAnalyzer(config)
        progressionEngine = ProgressionEngine(historyAnalyzer)
        planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
    }

    @Test
    fun `production scenario jan 18 with jan 16 history schedules squats`() {
        // "Today" is Jan 18
        val cal = GregorianCalendar(2024, Calendar.JANUARY, 18, 10, 0)
        val today = cal.time

        // History: Jan 16 (2 days ago)
        val historyCal = GregorianCalendar(2024, Calendar.JANUARY, 16, 10, 0)
        val historyTime = historyCal.timeInMillis

        // Single non-skipped set of squats
        val log1 = WorkoutLogEntry(
            sessionId = 1,
            exerciseName = "squat",
            targetReps = 8,
            targetDurationSeconds = null,
            loadDescription = "100",
            actualReps = 8,
            actualDurationSeconds = 120,
            rpe = null,
            notes = "Completed",
            timestamp = historyTime,
            skipped = false,
            tempo = null
        )

        // Skipped set of squats (should not contribute to fatigue, or handled by logic)
        // Usually skipped logs don't contribute fatigue in HistoryAnalyzer (filter { !it.skipped } check?)
        // Let's check HistoryAnalyzer source or assume skipped=true means actualDuration/reps are ignored or load is 0.
        // Reading HistoryAnalyzer.kt earlier:
        // getAccumulatedFatigue iterates history. It doesn't explicitly filter skipped?
        // Let's re-read HistoryAnalyzer.kt to be sure.
        // But for this test I'll add it as described.
        val log2 = WorkoutLogEntry(
            sessionId = 1,
            exerciseName = "squat",
            targetReps = 8,
            targetDurationSeconds = null,
            loadDescription = "100",
            actualReps = 0,
            actualDurationSeconds = 0,
            rpe = null,
            notes = "Skipped",
            timestamp = historyTime,
            skipped = true,
            tempo = null
        )

        val history = listOf(log1, log2)
        val schedule = ScheduleEntry("2024-01-18", today.time, 50, "gym")

        val plan = planner.generatePlan(today, history, schedule)

        if (plan.isEmpty()) {
            throw RuntimeException("Generated plan is empty")
        }

        // The first exercise should be 'squat' (from patellar_tendon priority)
        // NOT 'banded_x_walks' (from hip_abductors)
        assertEquals("squat", plan[0].exerciseName)
    }
}
