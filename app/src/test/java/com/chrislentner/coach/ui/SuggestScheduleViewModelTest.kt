package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.BlockExecution
import com.chrislentner.coach.planner.Plan
import com.chrislentner.coach.planner.WorkoutStep
import com.chrislentner.coach.planner.model.CoachConfig
import com.chrislentner.coach.planner.model.PriorityGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SuggestScheduleViewModelTest {

    private lateinit var viewModel: SuggestScheduleViewModel
    private lateinit var dao: WorkoutViewModelTest.FakeWorkoutDao
    private lateinit var repository: WorkoutRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var planner: AdvancedWorkoutPlanner
    private lateinit var config: CoachConfig

    @Before
    fun setup() {
        dao = WorkoutViewModelTest.FakeWorkoutDao()
        repository = WorkoutRepository(dao)
        scheduleRepository = mock(ScheduleRepository::class.java)
        planner = mock(AdvancedWorkoutPlanner::class.java)

        // Mock Config
        config = mock(CoachConfig::class.java)

        val mockBlock = com.chrislentner.coach.planner.model.Block(
            blockName = "BlockA",
            sizeMinutes = emptyList(),
            location = "Home",
            tags = emptyList(),
            prescription = emptyList(),
            progression = null
        )
        val pGroup = PriorityGroup(listOf(mockBlock))

        whenever(planner.config).thenReturn(config)
        whenever(config.priorityOrder).thenReturn(listOf("P1"))
        whenever(config.priorities).thenReturn(mapOf("P1" to pGroup))
    }

    @Test
    fun `generateSchedule generates 7 days plan`() {
        runBlocking {
            // Setup Schedule: Return null (default logic will run)
            whenever(scheduleRepository.getScheduleByDate(any())).thenReturn(null)

            // Mock generatePlan
            whenever(planner.generatePlan(any(), any(), any())).thenReturn(
                Plan(
                    steps = listOf(WorkoutStep("Test", 10, null, "Load")),
                    logs = emptyList(),
                    blocks = listOf(BlockExecution("BlockA", 10.0))
                )
            )

            viewModel = SuggestScheduleViewModel(repository, scheduleRepository, planner, Dispatchers.Unconfined)

            // Wait for coroutines
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(7, viewModel.suggestedPlans.size)
            assertTrue(viewModel.suggestedPlans.all { !it.isRestDay })
        }
    }
}
