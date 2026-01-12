package com.chrislentner.coach.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Use a specific SDK version if needed, or rely on manifest
class WorkoutDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build() // Allow main thread queries for testing
        workoutDao = database.workoutDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetWorkoutByDate() = runBlocking {
        val workout = WorkoutEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        workoutDao.insertOrUpdate(workout)

        val retrievedWorkout = workoutDao.getWorkoutByDate("2023-10-27")
        assertNotNull(retrievedWorkout)
        assertEquals(workout, retrievedWorkout)
    }

    @Test
    fun insertAndGetLastWorkout() = runBlocking {
        val workout1 = WorkoutEntry(
            date = "2023-10-26",
            timeInMillis = 1698278400000L,
            durationMinutes = 45,
            location = "Home"
        )
        val workout2 = WorkoutEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        workoutDao.insertOrUpdate(workout1)
        workoutDao.insertOrUpdate(workout2)

        val lastWorkout = workoutDao.getLastWorkout()
        assertNotNull(lastWorkout)
        assertEquals(workout2, lastWorkout)
    }

    @Test
    fun getWorkoutByDateReturnsNullWhenNotFound() = runBlocking {
        val workout = workoutDao.getWorkoutByDate("2023-10-27")
        assertNull(workout)
    }

    @Test
    fun updateExistingWorkout() = runBlocking {
        val workout = WorkoutEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        workoutDao.insertOrUpdate(workout)

        val updatedWorkout = workout.copy(durationMinutes = 90)
        workoutDao.insertOrUpdate(updatedWorkout)

        val retrievedWorkout = workoutDao.getWorkoutByDate("2023-10-27")
        assertNotNull(retrievedWorkout)
        assertEquals(90, retrievedWorkout?.durationMinutes)
    }
}
