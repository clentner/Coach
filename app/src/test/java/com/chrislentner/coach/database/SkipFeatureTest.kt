package com.chrislentner.coach.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SkipFeatureTest {

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
    fun skippedSetsShouldNotBeCountedInSummary() = runBlocking {
        // 1. Insert Session
        val session = WorkoutSession(
            date = "2023-11-01",
            startTimeInMillis = 1698800000000L,
            isCompleted = false,
            location = "Gym"
        )
        val sessionId = workoutDao.insertSession(session)

        // 2. Insert Completed Log Entry
        val completedEntry = WorkoutLogEntry(
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
            timestamp = 1698800001000L
        )
        workoutDao.insertLogEntry(completedEntry)

        // 3. Insert Skipped Log Entry
        val skippedEntry = WorkoutLogEntry(
            sessionId = sessionId,
            exerciseName = "Bench Press",
            targetReps = 5,
            targetDurationSeconds = null,
            loadDescription = "135 lbs",
            actualReps = 5, // Copied as per req
            actualDurationSeconds = null,
            rpe = null,
            notes = null,
            skipped = true,
            timestamp = 1698800002000L
        )
        workoutDao.insertLogEntry(skippedEntry)

        // 4. Verify getLogsForSession returns both (2 total)
        val logs = workoutDao.getLogsForSession(sessionId)
        assertEquals("Should return all logs including skipped", 2, logs.size)

        // 5. Verify getSessionsWithSetCounts returns count of 1 (excluding skipped)
        val summaries = workoutDao.getSessionsWithSetCounts()
        assertEquals("Should return 1 session summary", 1, summaries.size)

        // This assertion is expected to fail initially as currently it counts all logs (2)
        assertEquals("Set count should exclude skipped sets", 1, summaries[0].setCount)
    }
}
