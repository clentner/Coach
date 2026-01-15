package com.chrislentner.coach.ui

import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutDao
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutViewModelTest {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var repository: WorkoutRepository

    @Before
    fun setup() {
        fakeDao = FakeWorkoutDao()
        repository = WorkoutRepository(fakeDao)
        viewModel = WorkoutViewModel(repository, null)
    }

    @Test
    fun skipCurrentStep_shouldNotResetTimer_whenTimerIsNotRunning() {
        // Initial state: Timer is NOT running
        assertFalse(viewModel.isTimerRunning)
        val initialStartTime = viewModel.timerStartTime
        assertEquals(null, initialStartTime)

        // Ensure ViewModel is initialized (coroutines run)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Call skip
        viewModel.skipCurrentStep()

        // Wait for coroutine
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertFalse("Timer should still be not running", viewModel.isTimerRunning)
        assertEquals("Timer start time should still be null", null, viewModel.timerStartTime)
    }

    @Test
    fun skipCurrentStep_shouldNotResetTimer_whenTimerIsRunning() {
        // Start the timer
        viewModel.toggleTimer()
        assertTrue(viewModel.isTimerRunning)
        val startTime = viewModel.timerStartTime
        assertNotNull(startTime)

        // Call skip
        viewModel.skipCurrentStep()

        // Wait for coroutine
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Timer should still be running", viewModel.isTimerRunning)

        // In the failing case (current behavior), it resets timer, so startTime changes (becomes new `now`).
        // In the passing case (desired behavior), startTime remains same.
        assertEquals("Timer start time should not change (reset)", startTime, viewModel.timerStartTime)
    }
}

class FakeWorkoutDao : WorkoutDao {
    private val sessions = mutableListOf<WorkoutSession>()
    private val logs = mutableListOf<WorkoutLogEntry>()

    override suspend fun insertSession(session: WorkoutSession): Long {
        val id = (sessions.size + 1).toLong()
        sessions.add(session.copy(id = id))
        return id
    }

    override suspend fun getSessionByDate(date: String): WorkoutSession? {
        return sessions.find { it.date == date }
    }

    override suspend fun insertLogEntry(entry: WorkoutLogEntry): Long {
        logs.add(entry)
        return logs.size.toLong()
    }

    override suspend fun deleteLogEntry(entry: WorkoutLogEntry) {
        logs.remove(entry)
    }

    override suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry> {
        return logs.filter { it.sessionId == sessionId }.sortedBy { it.timestamp }
    }

    override suspend fun getAllLogs(): List<WorkoutLogEntry> {
        return logs
    }

    override suspend fun getLogsSince(timestamp: Long): List<WorkoutLogEntry> {
        return logs.filter { it.timestamp >= timestamp }
    }

    override suspend fun getSessionsWithSetCounts(): List<SessionSummary> {
        return emptyList()
    }
}
