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
class ScheduleDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var scheduleDao: ScheduleDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build() // Allow main thread queries for testing
        scheduleDao = database.scheduleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetScheduleByDate() = runBlocking {
        val schedule = ScheduleEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        scheduleDao.insertOrUpdate(schedule)

        val retrievedSchedule = scheduleDao.getScheduleByDate("2023-10-27")
        assertNotNull(retrievedSchedule)
        assertEquals(schedule, retrievedSchedule)
    }

    @Test
    fun insertAndGetLastSchedule() = runBlocking {
        val schedule1 = ScheduleEntry(
            date = "2023-10-26",
            timeInMillis = 1698278400000L,
            durationMinutes = 45,
            location = "Home"
        )
        val schedule2 = ScheduleEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        scheduleDao.insertOrUpdate(schedule1)
        scheduleDao.insertOrUpdate(schedule2)

        val lastSchedule = scheduleDao.getLastSchedule()
        assertNotNull(lastSchedule)
        assertEquals(schedule2, lastSchedule)
    }

    @Test
    fun getScheduleByDateReturnsNullWhenNotFound() = runBlocking {
        val schedule = scheduleDao.getScheduleByDate("2023-10-27")
        assertNull(schedule)
    }

    @Test
    fun updateExistingSchedule() = runBlocking {
        val schedule = ScheduleEntry(
            date = "2023-10-27",
            timeInMillis = 1698364800000L,
            durationMinutes = 60,
            location = "Gym"
        )

        scheduleDao.insertOrUpdate(schedule)

        val updatedSchedule = schedule.copy(durationMinutes = 90)
        scheduleDao.insertOrUpdate(updatedSchedule)

        val retrievedSchedule = scheduleDao.getScheduleByDate("2023-10-27")
        assertNotNull(retrievedSchedule)
        assertEquals(90, retrievedSchedule?.durationMinutes)
    }
}
