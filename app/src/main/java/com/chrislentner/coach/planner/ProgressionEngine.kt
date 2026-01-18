package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.Progression
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ProgressionResult(
    val sizeMinutes: Int? = null,
    val loadDescription: String? = null
)

class ProgressionEngine(private val historyAnalyzer: HistoryAnalyzer) {

    private val NUMBER_REGEX = Regex("(\\d+(\\.\\d+)?)")

    fun determineProgression(block: Block, history: List<WorkoutLogEntry>): ProgressionResult {
        val progression = block.progression ?: return ProgressionResult()

        val pastSessions = historyAnalyzer.getLastSatisfyingSessions(block.blockName, history)

        return when (progression.type) {
            "linear_load" -> calculateLinearLoad(block, progression, pastSessions)
            "per_session_minutes" -> calculatePerSessionMinutes(block, progression, pastSessions)
            else -> ProgressionResult()
        }
    }

    private fun calculateLinearLoad(block: Block, progression: Progression, pastSessions: List<List<WorkoutLogEntry>>): ProgressionResult {
        val startLoad = (progression.parameters["start_load_lbs"] as? Number)?.toDouble() ?: 0.0
        val increment = (progression.parameters["increment_load_lbs"] as? Number)?.toDouble() ?: 0.0
        val everyN = progression.everyNSessions ?: 1

        if (pastSessions.isEmpty()) {
             return ProgressionResult(loadDescription = formatLoad(startLoad))
        }

        if (pastSessions.size < everyN) {
             val lastLoad = getLoadFromSession(pastSessions[0], block) ?: startLoad
             return ProgressionResult(loadDescription = formatLoad(lastLoad))
        }

        val recentSessions = pastSessions.take(everyN)
        val loads = recentSessions.map { getLoadFromSession(it, block) }

        if (loads.any { it == null }) {
             val lastLoad = loads.firstOrNull() ?: startLoad
             return ProgressionResult(loadDescription = formatLoad(lastLoad))
        }

        val validLoads = loads.filterNotNull()
        // Check if loads are consistent (ignoring small fp diffs)
        val firstLoad = validLoads.first()
        val isConsistent = validLoads.all { kotlin.math.abs(it - firstLoad) < 0.1 }

        if (isConsistent) {
            val nextLoad = firstLoad + increment
            return ProgressionResult(loadDescription = formatLoad(nextLoad))
        } else {
            return ProgressionResult(loadDescription = formatLoad(firstLoad))
        }
    }

    private fun calculatePerSessionMinutes(block: Block, progression: Progression, pastSessions: List<List<WorkoutLogEntry>>): ProgressionResult {
        val startMinutes = (progression.parameters["start_minutes"] as? Number)?.toInt() ?: 20
        val increment = (progression.parameters["increment_minutes"] as? Number)?.toInt() ?: 0
        val maxMinutes = (progression.parameters["max_minutes"] as? Number)?.toInt() ?: 60

        if (pastSessions.isEmpty()) {
            return ProgressionResult(sizeMinutes = startMinutes)
        }

        // Sum duration of relevant exercises in the last session
        val lastSession = pastSessions.first()

        // Filter for block exercises
        val blockExercises = block.prescription.map { it.exercise }.toSet()
        val relevantLogs = lastSession.filter { it.exerciseName in blockExercises }

        val lastDurationSeconds = relevantLogs.sumOf { it.actualDurationSeconds ?: 0 }
        val lastDurationMinutes = (lastDurationSeconds / 60.0).roundToInt() // Round to nearest minute

        // Logic: if I did 20 mins, next time 22.
        // But what if I did 10 mins (failed)?
        // Progression usually assumes successful completion.
        // `HistoryAnalyzer` heuristic already filtered "Satisfying Sessions".
        // For minutes blocks, we assumed >0 duration.
        // If I did 10 mins of 20 planned, `HistoryAnalyzer` might include it.
        // If I base next step on actual (10) -> next is 12.
        // If I base next step on *planned*? I don't know what was planned last time easily (it's not in logs).
        // Standard "Per Session Minutes" usually bases on Actual + Increment.
        // So 10 -> 12.

        val nextMinutes = min(maxMinutes, lastDurationMinutes + increment)

        // Ensure we don't go below startMinutes? Or just trust math?
        // Usually >= startMinutes
        val resultMinutes = max(startMinutes, nextMinutes)

        return ProgressionResult(sizeMinutes = resultMinutes)
    }

    private fun getLoadFromSession(sessionLogs: List<WorkoutLogEntry>, block: Block): Double? {
        val blockExercises = block.prescription.map { it.exercise }.toSet()
        // Find first log matching any block exercise
        val match = sessionLogs.firstOrNull { it.exerciseName in blockExercises && !it.skipped } ?: return null
        return parseLoad(match.loadDescription)
    }

    private fun parseLoad(description: String): Double? {
        val match = NUMBER_REGEX.find(description) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun formatLoad(load: Double): String {
        val str = if (load % 1.0 == 0.0) {
            load.toInt().toString()
        } else {
            load.toString()
        }
        return "$str lbs"
    }
}
