package com.chrislentner.coach.planner

import com.chrislentner.coach.database.Tempo
import com.chrislentner.coach.database.WorkoutLogEntry
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class WorkoutStep(
    val exerciseName: String,
    val targetReps: Int?,
    val targetDurationSeconds: Int?,
    val loadDescription: String,
    val tempo: Tempo? = null
)

object WorkoutPlanner {

    fun generatePlan(
        today: Date,
        history: List<WorkoutLogEntry>
    ): List<WorkoutStep> {
        // Placeholder logic:
        // - if no squats in yesterday's session (or skipped yesterday): plan 3 sets of squats at 85 lbs and 6s/rep for today
        // - else, if no CRACR in last 4 days, plan 2 sets of hamstring CRACR stretches today at 5 reps/leg
        // - else, plan 3 sets of 10 reps overhead press at 45 lbs

        val cal = Calendar.getInstance()
        cal.time = today
        // Strip time part for accurate date comparison if needed, but here we work with timestamps mostly.

        // "Yesterday" defined as [Today 00:00 - 24h, Today 00:00)
        // Or simply: check if any log entry in history falls in the "yesterday" window.

        // Let's define the windows relative to 'today'
        val msPerDay = TimeUnit.DAYS.toMillis(1)
        val todayMs = today.time

        // Find yesterday's start/end
        // NOTE: This simple subtraction assumes 'today' is effectively "now" or "start of today".
        // Ideally we'd normalize to midnight.
        // Let's assume 'today' passed in is the current execution time.

        // To verify "yesterday", we really need to check if there were squats on the calendar day before today.

        // Let's look for Squats in the last 24-48 hours window relative to now?
        // Or strictly "Yesterday's session".
        // Let's look for ANY squats in the history that have a timestamp within the "yesterday" calendar day.

        val yesterdayStart = getStartOfDay(today, -1)
        val yesterdayEnd = getStartOfDay(today, 0)

        val squatsYesterday = history.any {
            it.timestamp in yesterdayStart until yesterdayEnd &&
            it.exerciseName.equals("Squats", ignoreCase = true) &&
            !it.skipped
        }

        if (!squatsYesterday) {
            return List(3) {
                WorkoutStep(
                    exerciseName = "Squats",
                    targetReps = 5, // "6s/rep" implies tempo. Assuming 5 reps.
                    targetDurationSeconds = null,
                    loadDescription = "85 lbs",
                    tempo = Tempo(down = 3, up = 3)
                )
            }
        }

        // CRACR in last 4 days
        val fourDaysAgoStart = getStartOfDay(today, -4)
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
                    loadDescription = "Bodyweight"
                )
            }
        }

        // Else Overhead Press
        return List(3) {
            WorkoutStep(
                exerciseName = "Overhead Press",
                targetReps = 10,
                targetDurationSeconds = null,
                loadDescription = "45 lbs"
            )
        }
    }

    private fun getStartOfDay(date: Date, offsetDays: Int): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
