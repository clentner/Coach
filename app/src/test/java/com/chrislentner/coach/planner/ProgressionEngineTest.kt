package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.Prescription
import com.chrislentner.coach.planner.model.Progression
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

class ProgressionEngineTest {

    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var engine: ProgressionEngine

    @Before
    fun setup() {
        historyAnalyzer = mock(HistoryAnalyzer::class.java)
        engine = ProgressionEngine(historyAnalyzer)
    }

    @Test
    fun `linear load - no history returns start load`() {
        val block = createLinearBlock(100.0, 5.0)
        val history = emptyList<WorkoutLogEntry>()

        doReturn(emptyList<List<WorkoutLogEntry>>())
            .`when`(historyAnalyzer).getLastSatisfyingSessions(block.blockName, history)

        val result = engine.determineProgression(block, history)
        assertEquals("100 lbs", result.loadDescription)
    }

    @Test
    fun `linear load - consistent history increments load`() {
        val block = createLinearBlock(100.0, 5.0)
        val history = emptyList<WorkoutLogEntry>() // Dummy input history
        val logs = listOf(createLog("Squat", "100 lbs"))

        doReturn(listOf(logs))
            .`when`(historyAnalyzer).getLastSatisfyingSessions(block.blockName, history)

        val result = engine.determineProgression(block, history)
        assertEquals("105 lbs", result.loadDescription)
    }

    @Test
    fun `linear load - needs N sessions`() {
        val block = createLinearBlock(100.0, 5.0, everyN = 2)
        val history = emptyList<WorkoutLogEntry>()
        val logs = listOf(createLog("Squat", "100 lbs"))

        doReturn(listOf(logs))
            .`when`(historyAnalyzer).getLastSatisfyingSessions(block.blockName, history)

        val result = engine.determineProgression(block, history)
        // Should stay at 100 because we only returned 1 session in the history analyzer mock
        assertEquals("100 lbs", result.loadDescription)
    }

    @Test
    fun `per session minutes - increments duration`() {
        val block = createDurationBlock(20, 2, 40)
        val history = emptyList<WorkoutLogEntry>()
        val logs = listOf(createLog("Run", actualSeconds = 20 * 60))

        doReturn(listOf(logs))
            .`when`(historyAnalyzer).getLastSatisfyingSessions(block.blockName, history)

        val result = engine.determineProgression(block, history)
        assertEquals(22, result.sizeMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown progression throws exception`() {
        val block = Block(
            blockName = "Unknown",
            sizeMinutes = listOf(10),
            location = "Gym",
            contributesTo = emptyList(),
            prescription = emptyList(),
            progression = Progression(type = "magic_growth")
        )
        val history = emptyList<WorkoutLogEntry>()

        doReturn(emptyList<List<WorkoutLogEntry>>())
            .`when`(historyAnalyzer).getLastSatisfyingSessions(block.blockName, history)

        engine.determineProgression(block, history)
    }

    private fun createLinearBlock(start: Double, inc: Double, everyN: Int = 1): Block {
        return Block(
            blockName = "Linear",
            sizeMinutes = listOf(30),
            location = "Gym",
            contributesTo = emptyList(),
            prescription = listOf(Prescription(exercise = "Squat")),
            progression = Progression(
                type = "linear_load",
                everyNSessions = everyN,
                parameters = mapOf("start_load_lbs" to start, "increment_load_lbs" to inc)
            )
        )
    }

    private fun createDurationBlock(start: Int, inc: Int, max: Int): Block {
        return Block(
            blockName = "Cardio",
            sizeMinutes = listOf(start),
            location = "Outside",
            contributesTo = emptyList(),
            prescription = listOf(Prescription(exercise = "Run")),
            progression = Progression(
                type = "per_session_minutes",
                parameters = mapOf("start_minutes" to start, "increment_minutes" to inc, "max_minutes" to max)
            )
        )
    }

    private fun createLog(name: String, load: String = "", actualSeconds: Int = 0): WorkoutLogEntry {
        return WorkoutLogEntry(
            sessionId = 1,
            exerciseName = name,
            targetReps = 5,
            targetDurationSeconds = null,
            loadDescription = load,
            actualReps = 5,
            actualDurationSeconds = actualSeconds,
            rpe = null,
            notes = null,
            skipped = false,
            timestamp = 0L
        )
    }
}
