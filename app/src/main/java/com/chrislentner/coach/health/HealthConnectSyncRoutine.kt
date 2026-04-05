package com.chrislentner.coach.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.WorkoutSession
import com.chrislentner.coach.database.UserSettingsRepository
import com.chrislentner.coach.planner.ConfigLoader
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectSyncRoutine(
    private val context: Context,
    private val client: HealthConnectClient,
    private val workoutRepository: WorkoutRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    companion object {
        val TARGET_EXERCISE_TYPES = listOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, // 57
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING, // 68
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING // 8
        )
    }

    fun runSync(): Flow<String> = flow {
        emit("Starting Health Connect Sync...")

        val maxHr = userSettingsRepository.getUserSettings()?.maxHeartRate
        if (maxHr == null || maxHr < 100 || maxHr > 240) {
            emit("Error: Valid Max HR not found in user settings. Required for zone computation. Exiting.")
            return@flow
        }
        emit("Loaded Max HR: $maxHr")

        val config = ConfigLoader.load(context)
        val zoneTargets = config.targets
            .filter { it.type == "minutes" && it.id.startsWith("zone") }
            .mapNotNull { target ->
                val match = Regex("zone(\\d+)_minutes").find(target.id)
                if (match != null) {
                    match.groupValues[1].toInt() to target.id
                } else {
                    null
                }
            }
            .sortedByDescending { it.first } // Sort highest zone to lowest

        val targetsStr = zoneTargets.joinToString { "${it.second} (Zone ${it.first})" }
        emit("Loaded Zone Targets: $targetsStr")

        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

        emit("Fetching activities from the last 30 days...")
        val allActivities = HealthConnectManager.readExerciseSessions(client, thirtyDaysAgo, now)

        val filteredActivities = allActivities.filter { it.exerciseType in TARGET_EXERCISE_TYPES }
        emit("Found ${allActivities.size} total activities, ${filteredActivities.size} match target types.")

        if (filteredActivities.isEmpty()) {
            emit("No target activities found. Sync complete.")
            return@flow
        }

        for (activity in filteredActivities) {
            val typeLabel = HealthConnectLabels.exerciseTypeLabel(activity.exerciseType)
            val startInstant = activity.startTime
            val endInstant = activity.endTime
            val durationSecs = ChronoUnit.SECONDS.between(startInstant, endInstant)
            val startMillis = startInstant.toEpochMilli()

            emit("--- Processing Activity: $typeLabel at $startInstant, duration: ${durationSecs}s ---")

            // Check for duplicate log
            val localDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val dateStr = localDate.toString() // YYYY-MM-DD

            val allSessionsForDay = workoutRepository.getAllSessionsByDate(dateStr)
            var duplicateFound = false
            for (session in allSessionsForDay) {
                val logs = workoutRepository.getLogsForSession(session.id)
                for (log in logs) {
                    if (log.timestamp == startMillis && log.actualDurationSeconds == durationSecs.toInt()) {
                        duplicateFound = true
                        break
                    }
                }
                if (duplicateFound) break
            }

            if (duplicateFound) {
                emit("Skipping: Match found in DB for exact start time and duration.")
                continue
            }

            // Compute Zones
            emit("Fetching HR records for activity...")
            val hrRecords = HealthConnectManager.readHeartRateRecords(client, activity)
            val hrSamples = HealthConnectManager.flattenHeartRateSamples(hrRecords)
            val zoneResult = HealthConnectManager.computeHeartRateZones(activity, hrSamples, maxHr)

            val durations = zoneResult.durationsPerZone
            val customTargetsMapSecs = mutableMapOf<String, Long>()

            // Waterfall assignment
            for (zoneLevel in 5 downTo 1) {
                val durationForLevel = durations[zoneLevel] ?: 0L
                if (durationForLevel > 0) {
                    // Find highest available target <= current zone
                    val target = zoneTargets.find { it.first <= zoneLevel }
                    if (target != null) {
                        val currentSecs = customTargetsMapSecs[target.second] ?: 0L
                        customTargetsMapSecs[target.second] = currentSecs + durationForLevel
                    }
                }
            }

            val customTargetsMap = mutableMapOf<String, Double>()
            for ((tgt, secs) in customTargetsMapSecs) {
                val mins = secs / 60.0
                if (mins > 0) {
                    customTargetsMap[tgt] = mins
                }
            }

            val customTargetsJson = if (customTargetsMap.isNotEmpty()) {
                ObjectMapper().writeValueAsString(customTargetsMap)
            } else {
                null
            }
            emit("Zone Contributions: $customTargetsMap")

            // Find or Create Session
            var sessionToUse: WorkoutSession? = null

            // check for overlapping session
            for (session in allSessionsForDay) {
                val sessionStart = session.startTimeInMillis
                val sessionEnd = session.endTimeInMillis ?: Long.MAX_VALUE // open sessions extend indefinitely
                val activityEndMillis = startMillis + (durationSecs * 1000)

                // Overlap condition: startA < endB && endA > startB
                if (startMillis < sessionEnd && activityEndMillis > sessionStart) {
                    sessionToUse = session
                    break
                }
            }

            if (sessionToUse != null) {
                emit("Reusing overlapping session from $dateStr (ID: ${sessionToUse.id})")
            } else {
                // Determine local midnight for session timestamp
                val localMidnightMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // If we get here, no existing session overlapped. We explicitly create a new session
                // spanning just the activity itself.
                val newSession = WorkoutSession(
                    date = dateStr,
                    startTimeInMillis = startMillis,
                    endTimeInMillis = startMillis + (durationSecs * 1000),
                    isCompleted = true,
                    location = null
                )
                sessionToUse = workoutRepository.createSession(newSession)
                emit("Created new dedicated session for $dateStr (ID: ${sessionToUse.id})")
            }

            // Create Log
            val newLog = WorkoutLogEntry(
                sessionId = sessionToUse.id,
                exerciseName = typeLabel,
                targetReps = null,
                targetDurationSeconds = null,
                loadDescription = "",
                actualReps = null,
                actualDurationSeconds = durationSecs.toInt(),
                rpe = null,
                notes = "Imported from Health Connect",
                timestamp = startMillis,
                customTargets = customTargetsJson,
                customFatigue = null
            )

            workoutRepository.logSet(newLog)
            emit("Inserted new workout log entry for $typeLabel.")
        }

        emit("Sync Routine Complete.")
    }
}
