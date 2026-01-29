package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.CoachConfig
import com.chrislentner.coach.planner.model.Contribution
import com.chrislentner.coach.planner.model.FatigueConstraint
import com.chrislentner.coach.planner.model.Prescription
import com.chrislentner.coach.planner.model.PriorityGroup
import com.chrislentner.coach.planner.model.SelectionStrategy
import com.chrislentner.coach.planner.model.Target
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.Instant

class DeficitLogicTest {

    private lateinit var config: CoachConfig
    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var planner: AdvancedWorkoutPlanner
    private lateinit var progressionEngine: ProgressionEngine

    @Before
    fun setup() {
        // Setup Config
        val target = Target("test_target_sets", 7, "sets", 10)

        // Exercise contributing to target
        val prescription1 = Prescription(
            exercise = "ContribExercise",
            sets = 3,
            contributesTo = listOf(Contribution("test_target_sets"))
        )
        // Exercise NOT contributing
        val prescription2 = Prescription(
            exercise = "AccessoryExercise",
            sets = 3
        )

        val block = Block(
            blockName = "TestBlock",
            sizeMinutes = listOf(30),
            location = "Gym",
            prescription = listOf(prescription1, prescription2)
        )

        config = CoachConfig(
            version = 1,
            targets = listOf(target),
            fatigueConstraints = emptyMap(),
            priorityOrder = listOf("P1"),
            priorities = mapOf("P1" to PriorityGroup(listOf(block))),
            selection = SelectionStrategy("greedy")
        )

        historyAnalyzer = HistoryAnalyzer(config)
        progressionEngine = ProgressionEngine(historyAnalyzer)
        planner = AdvancedWorkoutPlanner(config, historyAnalyzer, progressionEngine)
    }

    @Test
    fun `HistoryAnalyzer counts performed from contributing exercise`() {
        val now = Instant.now()
        val history = listOf(
            // Should count
            WorkoutLogEntry(
                sessionId = 1,
                exerciseName = "ContribExercise",
                targetReps = 10,
                targetDurationSeconds = null,
                loadDescription = "",
                actualReps = 10,
                actualDurationSeconds = null,
                rpe = null,
                notes = null,
                skipped = false,
                timestamp = now.toEpochMilli()
            ),
            // Should NOT count
            WorkoutLogEntry(
                sessionId = 1,
                exerciseName = "AccessoryExercise",
                targetReps = 10,
                targetDurationSeconds = null,
                loadDescription = "",
                actualReps = 10,
                actualDurationSeconds = null,
                rpe = null,
                notes = null,
                skipped = false,
                timestamp = now.toEpochMilli()
            )
        )

        val performed = historyAnalyzer.getPerformed("test_target_sets", 7, now, history)
        assertEquals(1.0, performed, 0.0) // 1 set (log)
    }

    @Test
    fun `AdvancedWorkoutPlanner calculates reduction from contributing exercise`() {
        val now = Instant.now()
        val history = emptyList<WorkoutLogEntry>() // No history, full deficit
        val schedule = ScheduleEntry(
            date = "2023-01-01",
            isRestDay = false,
            timeInMillis = null,
            durationMinutes = 60,
            location = "Gym"
        )

        // Deficit is 10. Goal 10. Performed 0.

        val plan = planner.generatePlan(now, history, schedule)

        // Should schedule the block.
        // Block has 2 exercises (3 sets each).
        // Only "ContribExercise" contributes.
        // It has 3 sets.
        // So reduction should be 3.
        // 10 - 3 = 7.
        // Wait, loop runs until no block added.
        // Block size 30. Available 60.
        // 1st pass: Deficit 10. Add block. Deficit becomes 7. Time 30.
        // 2nd pass: Deficit 7. Add block. Deficit becomes 4. Time 0.
        // 2 blocks scheduled. Total 2 * 3 = 6 sets reduced?

        // Wait, calculateReduction Logic:
        // returns relevantSteps.size.
        // steps are created for each set.
        // sets=3. So 3 steps.

        // Let's verify executedBlocks reduction value.

        val executions = plan.blocks
        assertEquals(1, executions.size)
        assertEquals(3.0, executions[0].reduction, 0.0)
    }
}
