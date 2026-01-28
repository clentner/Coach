package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutDao
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PastWorkoutsViewModelTest {

    private lateinit var dao: FakeWorkoutDao
    private lateinit var repository: WorkoutRepository
    private lateinit var viewModel: PastWorkoutsViewModel

    class FakeWorkoutDao : WorkoutDao {
        val sessionsFlow = MutableStateFlow<List<SessionSummary>>(emptyList())
        val sessions = mutableListOf<SessionSummary>()

        fun emitSessions() {
            sessionsFlow.value = ArrayList(sessions)
        }

        override fun getSessionsWithSetCountsFlow(): Flow<List<SessionSummary>> {
            return sessionsFlow
        }

        override suspend fun getSessionsWithSetCounts(): List<SessionSummary> = sessions
        override suspend fun insertSession(session: WorkoutSession): Long {
            sessions.add(SessionSummary(session.id, session.date, session.location, 0))
            emitSessions()
            return session.id
        }

        // Unused methods stubbed
        override suspend fun getSessionByDate(date: String) = null
        override suspend fun getSessionById(id: Long) = null
        override suspend fun getLogById(id: Long) = null
        override suspend fun insertLogEntry(entry: WorkoutLogEntry) = 0L
        override suspend fun updateLogEntry(entry: WorkoutLogEntry) {}
        override suspend fun deleteLogEntry(entry: WorkoutLogEntry) {}
        override suspend fun getLogsForSession(sessionId: Long) = emptyList<WorkoutLogEntry>()
        override fun getLogsForSessionFlow(sessionId: Long): Flow<List<WorkoutLogEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun getAllLogs() = emptyList<WorkoutLogEntry>()
        override suspend fun getLogsSince(timestamp: Long) = emptyList<WorkoutLogEntry>()
        override suspend fun getRecentExerciseNames(limit: Int) = emptyList<String>()
        override suspend fun getLastLogForExercise(exerciseName: String): WorkoutLogEntry? = null
    }

    @Before
    fun setup() {
        dao = FakeWorkoutDao()
        repository = WorkoutRepository(dao)
    }

    @Test
    fun `sessions update when flow emits`() {
        viewModel = PastWorkoutsViewModel(repository)

        // Initial state
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, viewModel.sessions.size)

        // Add session (via ViewModel logic roughly, but here we just manually poke DAO)
        val newSession = SessionSummary(id=1, date="2023-01-01", location="Gym", setCount=5)
        dao.sessions.add(newSession)
        dao.emitSessions()

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, viewModel.sessions.size)
        assertEquals("Gym", viewModel.sessions[0].location)
    }
}
