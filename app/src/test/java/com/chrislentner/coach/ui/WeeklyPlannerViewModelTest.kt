package com.chrislentner.coach.ui

import android.os.Looper
import com.chrislentner.coach.database.ScheduleDao
import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.ScheduleRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
class WeeklyPlannerViewModelTest {

    private lateinit var dao: FakeScheduleDao
    private lateinit var repository: ScheduleRepository

    class FakeScheduleDao : ScheduleDao {
        val entries = mutableListOf<ScheduleEntry>()

        override suspend fun getScheduleByDate(date: String) = entries.find { it.date == date }

        override suspend fun getScheduleBetweenDates(startDate: String, endDate: String): List<ScheduleEntry> {
            return entries.filter { it.date >= startDate && it.date <= endDate }
        }

        override suspend fun getLastSchedule() = entries.maxByOrNull { it.date }

        override suspend fun insertOrUpdate(entry: ScheduleEntry) {
            entries.removeIf { it.date == entry.date }
            entries.add(entry)
        }
    }

    @Before
    fun setup() {
        dao = FakeScheduleDao()
        repository = ScheduleRepository(dao)
    }

    @Test
    fun `initial load populates 7 days`() {
        val viewModel = WeeklyPlannerViewModel(repository)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(7, viewModel.days.size)

        // Verify dates are consecutive
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertEquals(today, viewModel.days[0].date)
    }

    @Test
    fun `toggleRestDay creates entry if none exists`() {
        val viewModel = WeeklyPlannerViewModel(repository)
        shadowOf(Looper.getMainLooper()).idle()

        val day = viewModel.days[0]
        assertFalse(day.isRestDay)

        viewModel.toggleRestDay(day)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(viewModel.days[0].isRestDay)

        val entry = dao.entries.find { it.date == day.date }
        assertNotNull(entry)
        assertTrue(entry!!.isRestDay)
    }

    @Test
    fun `toggleRestDay toggles existing entry`() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        dao.entries.add(ScheduleEntry(date = today, timeInMillis = null, durationMinutes = null, location = null, isRestDay = true))

        val viewModel = WeeklyPlannerViewModel(repository)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(viewModel.days[0].isRestDay)

        viewModel.toggleRestDay(viewModel.days[0])
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(viewModel.days[0].isRestDay)
        val entry = dao.entries.find { it.date == today }
        assertNotNull(entry)
        assertFalse(entry!!.isRestDay)
    }
}
