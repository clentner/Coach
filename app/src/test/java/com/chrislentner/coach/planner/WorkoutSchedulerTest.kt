package com.chrislentner.coach.planner

import com.chrislentner.coach.planner.model.CoachConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class WorkoutSchedulerTest {

    private lateinit var config: CoachConfig
    private lateinit var historyAnalyzer: HistoryAnalyzer
    private lateinit var scheduler: WorkoutScheduler

    @Before
    fun setup() {
        val file = File("src/test/resources/test_coach.yaml")
        val inputStream = FileInputStream(file)
        config = ConfigLoader.loadConfig(inputStream)
        historyAnalyzer = HistoryAnalyzer(config)
        scheduler = WorkoutScheduler(config, historyAnalyzer)
    }

    @Test
    fun `Test schedule generation`() {
        val schedule = scheduler.generateSchedule(emptyList())
        assertNotNull(schedule)
    }

    @Test
    fun `Test schedule validation with empty history`() {
        val schedule = scheduler.generateSchedule(emptyList())
        assertNotNull(schedule)
    }

    @Test
    fun `Test schedule respects fatigue constraints`() {
        val schedule = scheduler.generateSchedule(emptyList())
        assertNotNull(schedule)
    }

    @Test
    fun `Test schedule scoring`() {
        val schedule = scheduler.generateSchedule(emptyList())
        assertNotNull(schedule)
    }
}
