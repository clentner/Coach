package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutDao
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

        override suspend fun getRecentExerciseNames(limit: Int): List<String> {
            return emptyList()
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
    fun `updateCurrentStepTempo disables metronome when tempo is removed`() {
        // Ensure enabled initially (default is true)
        assertTrue(viewModel.isMetronomeEnabled)

        viewModel.updateCurrentStepTempo(null)

        assertFalse(viewModel.isMetronomeEnabled)
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
}
