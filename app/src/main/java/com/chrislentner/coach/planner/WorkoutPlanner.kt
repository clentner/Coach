package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutStep(
    val exerciseName: String,
    val targetReps: Int?,
    val targetDurationSeconds: Int?,
    val loadDescription: String,
    val tempo: String? = null
) {
    init {
        if (tempo != null) {
            require(tempo.matches(Regex("\\d{4}"))) { "Tempo must be a 4-digit string (e.g., '3030')" }
        }
    }
}

object WorkoutPlanner {

    val DEFAULT_EXERCISES = listOf(
        "Squats",
        "Deadlift",
        "Bench Press",
        "Overhead Press",
        "Pull Up",
        "Dumbbell Row",
        "Hamstring CRACR",
        "Lunge",
        "Plank"
    )

    fun generatePlan(
        today: Instant,
        history: List<WorkoutLogEntry>
    ): List<WorkoutStep> {
        val zoneId = ZoneId.systemDefault()
        val todayDate = today.atZone(zoneId).toLocalDate()
        // Strip time part for accurate date comparison if needed, but here we work with timestamps mostly.

        // "Yesterday" defined as [Today 00:00 - 24h, Today 00:00)
        // Or simply: check if any log entry in history falls in the "yesterday" window.

        // Let's define the windows relative to 'today'
        // Find yesterday's start/end
        // NOTE: This simple subtraction assumes 'today' is effectively "now" or "start of today".
        // Ideally we'd normalize to midnight.
        // Let's assume 'today' passed in is the current execution time.

        // To verify "yesterday", we really need to check if there were squats on the calendar day before today.

        // Let's look for Squats in the last 24-48 hours window relative to now?
        // Or strictly "Yesterday's session".
        // Let's look for ANY squats in the history that have a timestamp within the "yesterday" calendar day.

        val yesterdayStart = getStartOfDay(todayDate, zoneId, -1)
        val yesterdayEnd = getStartOfDay(todayDate, zoneId, 0)

        val squatsYesterday = history.any {
            it.timestamp in yesterdayStart until yesterdayEnd &&
            it.exerciseName.equals("Squats", ignoreCase = true) &&
            !it.skipped
        }

        if (!squatsYesterday) {
            return List(3) {
                WorkoutStep(
                    exerciseName = "Squats",
                    targetReps = 5,
                    targetDurationSeconds = null,
                    loadDescription = "85 lbs",
                    tempo = "3030"
                )
            }
        }

        // CRACR in last 4 days
        val fourDaysAgoStart = getStartOfDay(todayDate, zoneId, -4)
        val cracrRecent = history.any {
            it.timestamp >= fourDaysAgoStart &&
            it.exerciseName.contains("CRACR", ignoreCase = true) &&
            !it.skipped
        }

        if (!cracrRecent) {
             return List(2) {
                WorkoutStep(
                    exerciseName = "Hamstring CRACR",
                    targetReps = 5, // "5 reps/leg" -> 5 reps. Note: "per leg" logic not handled in simple model.
                    targetDurationSeconds = null,
                    loadDescription = "Bodyweight",
                    tempo = null
                )
            }
        }

        // Else Overhead Press
        return List(3) {
            WorkoutStep(
                exerciseName = "Overhead Press",
                targetReps = 10,
                targetDurationSeconds = null,
                loadDescription = "45 lbs",
                tempo = null
            )
        }
    }

    private fun getStartOfDay(date: LocalDate, zoneId: ZoneId, offsetDays: Int): Long {
        return date.plusDays(offsetDays.toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
