package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.SessionSummary
import com.chrislentner.coach.database.WorkoutDao
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WorkoutDetailViewModelTest {

    private lateinit var dao: FakeWorkoutDao
    private lateinit var repository: WorkoutRepository
    private lateinit var viewModel: WorkoutDetailViewModel

    class FakeWorkoutDao : WorkoutDao {
        val logsFlow = MutableStateFlow<List<WorkoutLogEntry>>(emptyList())
        val logs = mutableListOf<WorkoutLogEntry>()

        fun emitLogs() {
            logsFlow.value = ArrayList(logs)
        }

        override suspend fun insertLogEntry(entry: WorkoutLogEntry): Long {
            logs.add(entry)
            emitLogs()
            return entry.id
        }

        override suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry> {
            return logs.filter { it.sessionId == sessionId }
        }

        override fun getLogsForSessionFlow(sessionId: Long): Flow<List<WorkoutLogEntry>> {
            return logsFlow.map { allLogs ->
                allLogs.filter { it.sessionId == sessionId }
            }
        }

        // Unused methods stubbed
        override suspend fun insertSession(session: WorkoutSession) = 0L
        override suspend fun getSessionByDate(date: String) = null
        override suspend fun getSessionById(id: Long) = null
        override suspend fun getLogById(id: Long) = null
        override suspend fun updateLogEntry(entry: WorkoutLogEntry) {}
        override suspend fun deleteLogEntry(entry: WorkoutLogEntry) {}
        override suspend fun getAllLogs() = emptyList<WorkoutLogEntry>()
        override suspend fun getLogsSince(timestamp: Long) = emptyList<WorkoutLogEntry>()
        override suspend fun getSessionsWithSetCounts() = emptyList<SessionSummary>()
        override suspend fun getRecentExerciseNames(limit: Int) = emptyList<String>()
    }

    @Before
    fun setup() {
        dao = FakeWorkoutDao()
        repository = WorkoutRepository(dao)
    }

    @Test
    fun `logs update when flow emits`() {
        val sessionId = 1L
        viewModel = WorkoutDetailViewModel(repository, sessionId)

        // Initial state
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, viewModel.logs.size)

        // Add log
        val log = WorkoutLogEntry(
            id=1, sessionId=sessionId, exerciseName="Test",
            targetReps=10, targetDurationSeconds=null,
            loadDescription="100", timestamp=1000L,
            actualReps=10, actualDurationSeconds=null,
            rpe=null, notes=null, skipped=false
        )

        // Directly add to DAO and trigger emission
        dao.logs.add(log)
        dao.emitLogs()

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, viewModel.logs.size)
        assertEquals("Test", viewModel.logs[0].exerciseName)
    }
}
