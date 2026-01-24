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
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

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
    fun `uses intent destination when provided`() {
        val viewModel = MainViewModel(repository, "custom_dest")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("custom_dest", state.startDestination)
    }

    @Test
    fun `defaults to home when schedule exists for today`() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        dao.entries.add(ScheduleEntry(date = today, timeInMillis = null, durationMinutes = null, location = null, isRestDay = false))

        val viewModel = MainViewModel(repository, null)
        shadowOf(Looper.getMainLooper()).idle() // Wait for coroutine

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("home", state.startDestination)
    }

    @Test
    fun `goes to survey when no schedule exists for today`() {
        // No entries in dao

        val viewModel = MainViewModel(repository, null)
        shadowOf(Looper.getMainLooper()).idle() // Wait for coroutine

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("survey", state.startDestination)
    }
}
