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
class EditExerciseViewModelTest {

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
        override suspend fun getSessionByDate(date: String) = sessions.find { it.date == date }
        override suspend fun insertLogEntry(entry: WorkoutLogEntry): Long {
            val id = (logs.size + 1).toLong()
            logs.add(entry.copy(id = id))
            return id
        }
        override suspend fun updateLogEntry(entry: WorkoutLogEntry) {
            val index = logs.indexOfFirst { it.id == entry.id }
            if (index != -1) logs[index] = entry
        }
        override suspend fun getLogById(id: Long) = logs.find { it.id == id }
        override suspend fun getSessionById(id: Long) = sessions.find { it.id == id }
        override suspend fun deleteLogEntry(entry: WorkoutLogEntry) { logs.removeIf { it.id == entry.id } }
        override suspend fun getLogsForSession(sessionId: Long) = logs.filter { it.sessionId == sessionId }
        override suspend fun getAllLogs() = logs
        override suspend fun getLogsSince(timestamp: Long) = logs.filter { it.timestamp >= timestamp }
        override suspend fun getSessionsWithSetCounts() = emptyList<SessionSummary>()
        override suspend fun getRecentExerciseNames(limit: Int) = emptyList<String>()
        override suspend fun getAllExerciseNamesOrderedByRecency() = logs.sortedByDescending { it.timestamp }.map { it.exerciseName }.distinct()
    }

    @Before
    fun setup() {
        dao = FakeWorkoutDao()
        repository = WorkoutRepository(dao)
    }

    @Test
    fun `save creates new log with session timestamp`() {
        // Setup session
        val sessionTimestamp = 1000L
        val session = WorkoutSession(id=1, date="2023-01-01", startTimeInMillis=sessionTimestamp, isCompleted=false)
        dao.sessions.add(session)

        val viewModel = EditExerciseViewModel(repository, sessionId=1, logId=null)
        viewModel.exerciseName = "Squat"
        viewModel.load = "100"
        viewModel.reps = "5"

        var successCalled = false
        viewModel.save { successCalled = true }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(successCalled)
        assertEquals(1, dao.logs.size)
        val log = dao.logs.first()
        assertEquals("Squat", log.exerciseName)
        assertEquals(sessionTimestamp, log.timestamp)
    }

    @Test
    fun `save updates existing log`() {
         // Setup session & log
        val session = WorkoutSession(id=1, date="2023-01-01", startTimeInMillis=1000L, isCompleted=false)
        dao.sessions.add(session)
        val existingLog = WorkoutLogEntry(id=1, sessionId=1, exerciseName="Squat", targetReps=5, targetDurationSeconds=null, loadDescription="100", timestamp=1000L, actualReps=5, actualDurationSeconds=null, rpe=null, notes=null)
        dao.logs.add(existingLog)

        val viewModel = EditExerciseViewModel(repository, sessionId=1, logId=1)
        shadowOf(Looper.getMainLooper()).idle() // let init run

        assertEquals("Squat", viewModel.exerciseName)

        viewModel.exerciseName = "Bench"
        var successCalled = false
        viewModel.save { successCalled = true }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(successCalled)
        val log = dao.logs.first()
        assertEquals("Bench", log.exerciseName)
        assertEquals(1000L, log.timestamp)
    }
}
