package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.CoachConfig
import com.chrislentner.coach.planner.model.Schedule
import com.chrislentner.coach.planner.model.ScheduledSession
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val MAX_SESSION_DURATION_MINUTES = 90
private const val WINDOW_DAYS = 7

class WorkoutScheduler(
    private val config: CoachConfig,
    private val historyAnalyzer: HistoryAnalyzer
) {

    private val allBlocks = config.priorities.values.flatMap { it.blocks }
    private val possibleSessions = generateAllPossibleSessions()

    fun generateSchedule(history: List<WorkoutLogEntry>): Schedule {
        var bestSchedule = createEmptySchedule()
        var bestScore = scoreSchedule(bestSchedule, history)

        // Basic hill-climbing algorithm
        for (i in 0..100) { // Number of iterations
            var newSchedule = bestSchedule.copy(sessions = bestSchedule.sessions.toMutableList())

            // Make a random change to the schedule
            val dayToChange = Random.nextInt(WINDOW_DAYS)
            val randomSession = possibleSessions.randomOrNull()

            if (randomSession != null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, dayToChange)
                val newSession = ScheduledSession(
                    date = calendar.time,
                    location = randomSession.location,
                    blocks = randomSession.blocks,
                    durationMinutes = randomSession.durationMinutes
                )
                // Replace or add the session
                val existingSessionIndex = newSchedule.sessions.indexOfFirst {
                    val cal1 = Calendar.getInstance().apply { time = it.date }
                    val cal2 = Calendar.getInstance().apply { time = newSession.date }
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }
                if (existingSessionIndex != -1) {
                    (newSchedule.sessions as MutableList)[existingSessionIndex] = newSession
                } else {
                    (newSchedule.sessions as MutableList).add(newSession)
                }
            } else {
                // Remove a session
                if (newSchedule.sessions.isNotEmpty()) {
                    (newSchedule.sessions as MutableList).removeAt(Random.nextInt(newSchedule.sessions.size))
                }
            }

            if (validateSchedule(newSchedule, history)) {
                val newScore = scoreSchedule(newSchedule, history)
                if (newScore > bestScore) {
                    bestSchedule = newSchedule
                    bestScore = newScore
                }
            }
        }
        return bestSchedule
    }

    private fun createEmptySchedule(): Schedule {
        return Schedule(sessions = emptyList())
    }

    private fun generateAllPossibleSessions(): List<ScheduledSession> {
        val sessions = mutableListOf<ScheduledSession>()
        val locations = allBlocks.map { it.location }.distinct()

        for (location in locations) {
            val blocksForLocation = allBlocks.filter { it.location == location || it.location == "anywhere" }
            // Generate combinations of blocks
            for (i in 1..blocksForLocation.size) {
                val combinations = blocksForLocation.combinations(i)
                for (combination in combinations) {
                    val duration = combination.sumOf { it.sizeMinutes.firstOrNull() ?: 0 }
                    if (duration <= MAX_SESSION_DURATION_MINUTES) {
                        sessions.add(
                            ScheduledSession(
                                date = Date(), // Placeholder
                                location = location,
                                blocks = combination,
                                durationMinutes = duration
                            )
                        )
                    }
                }
            }
        }
        return sessions
    }

    private fun validateSchedule(schedule: Schedule, history: List<WorkoutLogEntry>): Boolean {
        val scheduleLogs = scheduleToLogs(schedule)
        val combinedHistory = history + scheduleLogs

        for (constraint in config.fatigueConstraints.values.flatten()) {
            val relevantBlocks = allBlocks.filter { it.tags.any { tag -> constraint.appliesToBlocksWithTag.contains(tag) } }
            val relevantExercises = relevantBlocks.flatMap { it.prescription }.map { it.exercise }.toSet()

            for (log in combinedHistory) {
                if (relevantExercises.contains(log.exerciseName)) {
                    val fatigue = historyAnalyzer.getAccumulatedFatigue(
                        constraint.kind,
                        constraint.windowHours,
                        Date(log.timestamp),
                        combinedHistory
                    )
                    if (fatigue > constraint.threshold) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun scoreSchedule(schedule: Schedule, history: List<WorkoutLogEntry>): Double {
        var score = 0.0
        val scheduleLogs = scheduleToLogs(schedule)
        val combinedHistory = history + scheduleLogs

        for (target in config.targets.sortedBy { config.priorityOrder.indexOf(it.id) }) {
            for (i in 0..WINDOW_DAYS) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, i)
                val windowEndDate = calendar.time
                if (historyAnalyzer.isTargetMet(target.id, target.windowDays, windowEndDate, combinedHistory)) {
                    score += 1.0
                }
            }
        }
        return score
    }

    private fun scheduleToLogs(schedule: Schedule): List<WorkoutLogEntry> {
        val logs = mutableListOf<WorkoutLogEntry>()
        schedule.sessions.forEachIndexed { index, session ->
            val sessionId = "scheduled-session-$index"
            session.blocks.flatMap { it.prescription }.forEach { prescription ->
                val sets = prescription.sets ?: 1
                repeat(sets) {
                    logs.add(
                        WorkoutLogEntry(
                            sessionId = sessionId.hashCode().toLong(),
                            exerciseName = prescription.exercise,
                            targetReps = prescription.reps,
                            targetDurationSeconds = prescription.seconds,
                            loadDescription = "",
                            tempo = prescription.tempo,
                            actualReps = prescription.reps,
                            actualDurationSeconds = prescription.seconds,
                            rpe = prescription.rpe,
                            notes = "",
                            skipped = false,
                            timestamp = session.date.time
                        )
                    )
                }
            }
        }
        return logs
    }

    // Helper function for combinations
    private fun <T> List<T>.combinations(size: Int): List<List<T>> {
        if (size == 0) return listOf(emptyList())
        if (isEmpty()) return emptyList()

        val first = first()
        val rest = drop(1)
        val combsWithFirst = rest.combinations(size - 1).map { it + first }
        val combsWithoutFirst = rest.combinations(size)
        return combsWithFirst + combsWithoutFirst
    }
}
