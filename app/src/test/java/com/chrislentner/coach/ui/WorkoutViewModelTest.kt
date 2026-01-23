package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutDao
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner
import com.chrislentner.coach.planner.WorkoutStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WorkoutViewModelTest {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var dao: FakeWorkoutDao
    private lateinit var repository: WorkoutRepository

    class FakeWorkoutDao : WorkoutDao {
        val sessions = mutableListOf<WorkoutSession>()
        val logs = mutableListOf<WorkoutLogEntry>()

        override suspend fun insertSession(session: WorkoutSession): Long {
            val id = (sessions.size + 1).toLong()
            sessions.add(session.copy(id = id))
            return id
        }

        override suspend fun getSessionByDate(date: String): WorkoutSession? {
            return sessions.find { it.date == date }
        }

        override suspend fun insertLogEntry(entry: WorkoutLogEntry): Long {
            val id = (logs.size + 1).toLong()
            logs.add(entry.copy(id = id))
            return id
        }

        override suspend fun updateLogEntry(entry: WorkoutLogEntry) {
            val index = logs.indexOfFirst { it.id == entry.id }
            if (index != -1) {
                logs[index] = entry
            }
        }

        override suspend fun getLogById(id: Long): WorkoutLogEntry? {
            return logs.find { it.id == id }
        }

        override suspend fun getSessionById(id: Long): WorkoutSession? {
            return sessions.find { it.id == id }
        }

        override suspend fun deleteLogEntry(entry: WorkoutLogEntry) {
            logs.removeIf { it.id == entry.id }
        }

        override suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry> {
            return logs.filter { it.sessionId == sessionId }.sortedBy { it.timestamp }
        }

        override suspend fun getAllLogs(): List<WorkoutLogEntry> {
            return logs.sortedByDescending { it.timestamp }
        }

        override suspend fun getLogsSince(timestamp: Long): List<WorkoutLogEntry> {
            return logs.filter { it.timestamp >= timestamp }.sortedByDescending { it.timestamp }
        }

        override suspend fun getSessionsWithSetCounts(): List<SessionSummary> {
            return emptyList()
        }

        override fun getSessionsWithSetCountsFlow(): Flow<List<SessionSummary>> {
            return flowOf(emptyList())
        }

        override suspend fun getRecentExerciseNames(limit: Int): List<String> {
            return emptyList()
        }

        override fun getLogsForSessionFlow(sessionId: Long): Flow<List<WorkoutLogEntry>> {
            return flowOf(logs.filter { it.sessionId == sessionId }.sortedBy { it.timestamp })
        }
    }

    @Before
    fun setup() {
        dao = FakeWorkoutDao()
        repository = WorkoutRepository(dao)
        viewModel = WorkoutViewModel(repository)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `initial state is Active`() {
        val state = viewModel.uiState
        assertTrue("State should be Active but was $state", state is WorkoutUiState.Active)
        val activeState = state as WorkoutUiState.Active
        assertEquals("Squats", activeState.currentStep?.exerciseName)
        assertEquals("3030", activeState.currentStep?.tempo)
    }

    @Test
    fun `updateCurrentStepTempo updates the tempo`() {
        val state = viewModel.uiState
        assertTrue("State should be Active", state is WorkoutUiState.Active)

        // Update to valid tempo
        viewModel.updateCurrentStepTempo("4010")

        val newState = viewModel.uiState as WorkoutUiState.Active
        assertEquals("4010", newState.currentStep?.tempo)
    }

    @Test
    fun `updateCurrentStepTempo allows null`() {
        val state = viewModel.uiState
        assertTrue("State should be Active", state is WorkoutUiState.Active)

        // Remove tempo
        viewModel.updateCurrentStepTempo(null)

        val newState = viewModel.uiState as WorkoutUiState.Active
        assertNull(newState.currentStep?.tempo)
    }

    @Test
    fun `toggleMetronome updates correct state based on hasTempo`() {
        // Initial state
        assertTrue("Metronome with tempo should be enabled by default", viewModel.isMetronomeEnabledWithTempo)
        assertFalse("Metronome without tempo should be disabled by default", viewModel.isMetronomeEnabledWithoutTempo)

        // Toggle for tempo
        viewModel.toggleMetronome(hasTempo = true)
        assertFalse("Metronome with tempo should be disabled after toggle", viewModel.isMetronomeEnabledWithTempo)
        assertFalse("Metronome without tempo should remain disabled", viewModel.isMetronomeEnabledWithoutTempo)

        // Toggle for no tempo
        viewModel.toggleMetronome(hasTempo = false)
        assertFalse("Metronome with tempo should remain disabled", viewModel.isMetronomeEnabledWithTempo)
        assertTrue("Metronome without tempo should be enabled after toggle", viewModel.isMetronomeEnabledWithoutTempo)
    }

    @Test
    fun `skipCurrentStep preserves running timer`() {
        // Start the timer
        viewModel.toggleTimer()
        assertTrue(viewModel.isTimerRunning)
        val initialStartTime = viewModel.timerStartTime
        assertNotNull(initialStartTime)

        // Advance time slightly to ensure we can distinguish between initial and potentially reset time
        Thread.sleep(10)

        // Skip the current step
        viewModel.skipCurrentStep()
        shadowOf(Looper.getMainLooper()).idle()

        // Verify timer is still running and start time hasn't changed
        assertTrue("Timer should still be running after skip", viewModel.isTimerRunning)
        assertEquals("Timer start time should not change after skip", initialStartTime, viewModel.timerStartTime)
    }

    @Test
    fun `skipCurrentStep preserves stopped timer`() {
        // Ensure timer is stopped
        if (viewModel.isTimerRunning) {
            viewModel.toggleTimer()
        }
        assertFalse(viewModel.isTimerRunning)
        assertNull(viewModel.timerStartTime)

        // Skip the current step
        viewModel.skipCurrentStep()
        shadowOf(Looper.getMainLooper()).idle()

        // Verify timer is still stopped
        assertFalse("Timer should remain stopped after skip", viewModel.isTimerRunning)
        assertNull("Timer start time should remain null after skip", viewModel.timerStartTime)
    }

    @Test
    fun `initializeSession only generates plan once`() {
        runBlocking {
            val planner = mock(AdvancedWorkoutPlanner::class.java)
            val scheduleRepo = mock(ScheduleRepository::class.java)
            // Stub scheduleRepo to return a schedule so planner is used
            val schedule = ScheduleEntry(date = "2024-01-01", timeInMillis = 1000L, location = "Gym", durationMinutes = 60)
            whenever(scheduleRepo.getScheduleByDate(any())).thenReturn(schedule)
            whenever(planner.generatePlan(any(), any(), any())).thenReturn(listOf(WorkoutStep("Test", 10, null, "Load")))

            viewModel = WorkoutViewModel(repository, scheduleRepo, planner)
            shadowOf(Looper.getMainLooper()).idle()

            verify(planner, times(1)).generatePlan(any(), any(), any())

            // Simulate completing a step which calls initializeSession again
            viewModel.completeCurrentStep()
            shadowOf(Looper.getMainLooper()).idle()

            // Should still be 1 interaction because cached plan is used
            verify(planner, times(1)).generatePlan(any(), any(), any())
        }
    }

    @Test
    fun `generatePlan excludes logs from today`() {
        runBlocking {
            val planner = mock(AdvancedWorkoutPlanner::class.java)
            val scheduleRepo = mock(ScheduleRepository::class.java)
            val schedule = ScheduleEntry(date = "2024-01-01", timeInMillis = 1000L, location = "Gym", durationMinutes = 60)
            whenever(scheduleRepo.getScheduleByDate(any())).thenReturn(schedule)
            whenever(planner.generatePlan(any(), any(), any())).thenReturn(emptyList())

            // Add a log for today and yesterday
            val now = System.currentTimeMillis()
            val yesterday = now - 24 * 60 * 60 * 1000
            val todayLog = WorkoutLogEntry(id=1, sessionId=1, exerciseName="Squats", targetReps=5, targetDurationSeconds=null, loadDescription="100", actualReps=5, actualDurationSeconds=null, rpe=null, notes=null, timestamp=now)
            val yesterdayLog = WorkoutLogEntry(id=2, sessionId=2, exerciseName="Squats", targetReps=5, targetDurationSeconds=null, loadDescription="100", actualReps=5, actualDurationSeconds=null, rpe=null, notes=null, timestamp=yesterday)

            dao.logs.add(todayLog)
            dao.logs.add(yesterdayLog)

            viewModel = WorkoutViewModel(repository, scheduleRepo, planner)
            shadowOf(Looper.getMainLooper()).idle()

            val captor = argumentCaptor<List<WorkoutLogEntry>>()
            verify(planner).generatePlan(any(), captor.capture(), any())

            val capturedHistory = captor.firstValue
            assertTrue("History should include yesterday's log", capturedHistory.any { it.id == yesterdayLog.id })
            assertFalse("History should NOT include today's log", capturedHistory.any { it.id == todayLog.id })
        }
    }
}
