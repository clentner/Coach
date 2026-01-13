package com.chrislentner.coach.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        workoutDao = database.workoutDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndDeleteLogEntry() = runBlocking {
        // 1. Insert Session
        val session = WorkoutSession(
            date = "2023-10-27",
            startTimeInMillis = 1698364800000L,
            isCompleted = false,
            location = "Gym"
        )
        val sessionId = workoutDao.insertSession(session)

        // 2. Insert Log Entry
        val logEntry = WorkoutLogEntry(
            sessionId = sessionId,
            exerciseName = "Squat",
            targetReps = 5,
            targetDurationSeconds = null,
            loadDescription = "100 lbs",
            actualReps = 5,
            actualDurationSeconds = null,
            rpe = 8,
            notes = null,
            skipped = false,
            timestamp = 1698364800000L
        )
        workoutDao.insertLogEntry(logEntry)

        // Verify it's there
        val logsBefore = workoutDao.getLogsForSession(sessionId)
        assertEquals(1, logsBefore.size)

        // 3. Delete Log Entry
        // We need the object with the ID populated. Since getLogsForSession returns entries with IDs.
        val entryToDelete = logsBefore[0]
        workoutDao.deleteLogEntry(entryToDelete)

        // 4. Verify it's gone
        val logsAfter = workoutDao.getLogsForSession(sessionId)
        assertTrue(logsAfter.isEmpty())
    }
}
