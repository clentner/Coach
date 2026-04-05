package com.chrislentner.coach.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateRecord.Sample
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.request.ReadRecordsRequest
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import com.chrislentner.coach.database.UserSettingsRepository
import com.chrislentner.coach.database.UserSettings
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.ZoneId

class HealthConnectSyncRoutineTest {

    private val context = mock<Context>()
    private val client = mock<HealthConnectClient>()
    private val workoutRepository = mock<WorkoutRepository>()
    private val userSettingsRepository = mock<UserSettingsRepository>()

    private lateinit var classUnderTest: HealthConnectSyncRoutine

    @Before
    fun setup() {
        classUnderTest = HealthConnectSyncRoutine(context, client, workoutRepository, userSettingsRepository)
    }

    @Test
    fun `sync exits early if max hr is invalid`() = runBlocking {
        whenever(userSettingsRepository.getUserSettings()).thenReturn(null)

        val logs = classUnderTest.runSync().toList()
        assertTrue(logs.any { it.contains("Valid Max HR not found") })
    }

    // Mocking the complex HealthConnectManager object methods is difficult because they are static methods on an object.
    // Instead we test the logic inside HealthConnectSyncRoutine assuming HealthConnectManager behaves normally (as it's an object).
    // Actually, HealthConnectManager is an object with regular functions, we can mock its behavior using Mockito inline if we want, but since it relies on client we can just let it run or mock the client.
    // Since HealthConnectManager.readExerciseSessions takes `client.readRecords`, we must mock the client.

    @Test
    fun `sync successfully processes an activity and creates a new session`() = runBlocking {
        // Setup User Settings
        whenever(userSettingsRepository.getUserSettings()).thenReturn(UserSettings(maxHeartRate = 180))

        // We also need to mock ConfigLoader.load(context), but it loads from context.assets
        // Since we can't easily mock the config loaded from assets in a unit test without Robolectric,
        // we'll use a mocked context that returns a fake InputStream.
        val fakeYaml = """
        version: 3
        targets:
          - id: zone2_minutes
            window_days: 7
            type: minutes
            goal: 90
          - id: zone4_minutes
            window_days: 7
            type: minutes
            goal: 20
        fatigue_constraints: {}
        priority_order: []
        priorities: {}
        selection:
          strategy: greedy_strict
        """.trimIndent()

        val assetManager = mock<android.content.res.AssetManager>()
        whenever(context.assets).thenReturn(assetManager)
        whenever(assetManager.open("coach.yaml")).thenReturn(fakeYaml.byteInputStream())

        // Setup HealthConnectClient Response for ExerciseSessions
        val startTime = Instant.parse("2023-10-25T10:00:00Z")
        val endTime = Instant.parse("2023-10-25T11:00:00Z")
        val sessionRecord = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = null,
            endTime = endTime,
            endZoneOffset = null,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
            title = "Morning Run",
            notes = null,
            metadata = Metadata(id = "sess1", dataOrigin = DataOrigin("com.test"))
        )

        val readSessionResponse = mock<ReadRecordsResponse<ExerciseSessionRecord>> {
            on { records } doReturn listOf(sessionRecord)
        }

        // Setup HealthConnectClient Response for HeartRateRecords
        // Create samples that trigger zones. Max HR 180.
        // Zone 4 is > 144. Zone 2 is > 108.
        val sample1 = Sample(startTime.plusSeconds(0), 150L) // Zone 4
        val sample2 = Sample(startTime.plusSeconds(300), 120L) // Zone 2
        val sample3 = Sample(startTime.plusSeconds(600), 100L) // Zone 1

        val hrRecord = HeartRateRecord(
            startTime = startTime,
            startZoneOffset = null,
            endTime = endTime,
            endZoneOffset = null,
            samples = listOf(sample1, sample2, sample3),
            metadata = Metadata(id = "hr1", dataOrigin = DataOrigin("com.test"))
        )

        val readHrResponse = mock<ReadRecordsResponse<HeartRateRecord>> {
            on { records } doReturn listOf(hrRecord)
        }
        // since Mockito sometimes gets confused by generic reified calls with same name, we can do consecutive returns
        whenever(client.readRecords(any<ReadRecordsRequest<out androidx.health.connect.client.records.Record>>())).thenReturn(readSessionResponse as Any as ReadRecordsResponse<androidx.health.connect.client.records.Record>).thenReturn(readHrResponse as Any as ReadRecordsResponse<androidx.health.connect.client.records.Record>)

        // Setup Repository Mocking
        whenever(workoutRepository.getAllSessionsByDate(any())).thenReturn(emptyList())
        val newSessionId = 99L
        val newSessionSlot = argumentCaptor<WorkoutSession>()
        whenever(workoutRepository.createSession(newSessionSlot.capture())).thenAnswer {
            (it.arguments[0] as WorkoutSession).copy(id = newSessionId)
        }

        val logSlot = argumentCaptor<WorkoutLogEntry>()
        whenever(workoutRepository.logSet(logSlot.capture())).thenReturn(Unit)

        // Run
        val logs = classUnderTest.runSync().toList()

        // Assertions
        assertTrue("Expected new dedicated session creation", logs.any { it.contains("Created new dedicated session for") })

        val createdSession = newSessionSlot.firstValue
        assertEquals(startTime.toEpochMilli(), createdSession.startTimeInMillis)
        assertEquals(endTime.toEpochMilli(), createdSession.endTimeInMillis)
        assertEquals(true, createdSession.isCompleted)

        val createdLog = logSlot.firstValue
        assertEquals(newSessionId, createdLog.sessionId)
        assertEquals("Running Treadmill", createdLog.exerciseName)
        assertEquals(3600, createdLog.actualDurationSeconds) // 1 hour
        assertNotNull(createdLog.customTargets)
        assertTrue(createdLog.customTargets!!.contains("zone4_minutes") || createdLog.customTargets!!.contains("zone2_minutes"))
    }
}
