package com.chrislentner.coach.ui

import com.chrislentner.coach.database.WorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class PastWorkoutsViewModelTest {

    @Test
    fun formatDate_currentYear_returnsMonthDay() {
        val repo = mock(WorkoutRepository::class.java)
        val viewModel = PastWorkoutsViewModel(repo)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Use a date that definitely exists in any year (e.g. May 20)
        val result = viewModel.formatDate("$currentYear-05-20")
        assertEquals("May 20", result)
    }

    @Test
    fun formatDate_pastYear_returnsMonthDayYear() {
        val repo = mock(WorkoutRepository::class.java)
        val viewModel = PastWorkoutsViewModel(repo)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val pastYear = currentYear - 1

        val result = viewModel.formatDate("$pastYear-05-20")
        assertEquals("May 20, $pastYear", result)
    }

    @Test
    fun formatDate_invalidFormat_returnsOriginal() {
        val repo = mock(WorkoutRepository::class.java)
        val viewModel = PastWorkoutsViewModel(repo)

        val result = viewModel.formatDate("invalid-date")
        assertEquals("invalid-date", result)
    }
}
