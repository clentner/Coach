package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.CoachConfig
import java.time.Duration
import java.time.Instant
import kotlin.math.max

data class TargetContribution(
    val log: WorkoutLogEntry,
    val value: Double // sets or minutes
)

data class FatigueContribution(
    val log: WorkoutLogEntry,
    val load: Double
)

class HistoryAnalyzer(private val config: CoachConfig) {

    private val allBlocks: List<Block> = config.priorities.values.flatMap { it.blocks }
    private val exerciseFatigueMap: Map<String, Map<String, Any>> = buildFatigueMap()
    private val targetContributingExercises: Map<String, Set<String>> = buildTargetContributors()

    private fun buildFatigueMap(): Map<String, Map<String, Any>> {
        val map = mutableMapOf<String, Map<String, Any>>()
        allBlocks.flatMap { it.prescription }.forEach {
            if (it.fatigueLoads.isNotEmpty()) {
                map[it.exercise] = it.fatigueLoads
            }
        }
        return map
    }

    private fun buildTargetContributors(): Map<String, Set<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        allBlocks.forEach { block ->
            block.prescription.forEach { prescription ->
                prescription.contributesTo.forEach { contribution ->
                    map.getOrPut(contribution.target) { mutableSetOf() }.add(prescription.exercise)
                }
            }
        }
        return map
    }

    private fun calculateFatigueLoad(log: WorkoutLogEntry, kind: String): Double? {
        val fatigueDef = exerciseFatigueMap[log.exerciseName]
        if (fatigueDef != null && fatigueDef.containsKey(kind)) {
            val valOrFormula = fatigueDef[kind]
            return if (valOrFormula is Number) {
                valOrFormula.toDouble()
            } else if (valOrFormula is String) {
                val minutes = (log.actualDurationSeconds ?: 0) / 60.0
                MathEvaluator.evaluate(valOrFormula, mapOf("performed_minutes" to minutes, "\$performed_minutes" to minutes))
            } else {
                0.0
            }
        }
        return null
    }

    fun getFatigueContributions(kind: String, windowHours: Int, now: Instant, history: List<WorkoutLogEntry>): List<FatigueContribution> {
        val cutoff = now.minus(Duration.ofHours(windowHours.toLong())).toEpochMilli()
        val nowMillis = now.toEpochMilli()

        return history.filter { it.timestamp in cutoff..nowMillis && !it.skipped }
            .mapNotNull { log ->
                val load = calculateFatigueLoad(log, kind)
                if (load != null) {
                    FatigueContribution(log, load)
                } else {
                    null
                }
            }
    }

    fun getAccumulatedFatigue(kind: String, windowHours: Int, now: Instant, history: List<WorkoutLogEntry>): Double {
        return getFatigueContributions(kind, windowHours, now, history).sumOf { it.load }
    }

    fun getTargetContributions(targetId: String, windowDays: Int, now: Instant, history: List<WorkoutLogEntry>): List<TargetContribution> {
        val targetConfig = config.targets.find { it.id == targetId } ?: return emptyList()
        val cutoff = now.minus(Duration.ofDays(windowDays.toLong())).toEpochMilli()
        val nowMillis = now.toEpochMilli()

        val contributingExercises = targetContributingExercises[targetId] ?: emptySet()

        return history.filter { it.timestamp in cutoff..nowMillis && contributingExercises.contains(it.exerciseName) && !it.skipped }
            .map { log ->
                val value = if (targetConfig.type == "sets") {
                    1.0
                } else if (targetConfig.type == "minutes") {
                    (log.actualDurationSeconds ?: 0) / 60.0
                } else {
                    0.0
                }
                TargetContribution(log, value)
            }
    }

    fun getPerformed(targetId: String, windowDays: Int, now: Instant, history: List<WorkoutLogEntry>): Double {
        return getTargetContributions(targetId, windowDays, now, history).sumOf { it.value }
    }

    fun getDeficit(targetId: String, windowDays: Int, now: Instant, history: List<WorkoutLogEntry>): Double {
         val targetConfig = config.targets.find { it.id == targetId } ?: return 0.0
         val performed = getPerformed(targetId, windowDays, now, history)
         return max(0.0, targetConfig.goal - performed)
    }

    fun getLastSatisfyingSessions(blockName: String, history: List<WorkoutLogEntry>): List<List<WorkoutLogEntry>> {
        val block = allBlocks.find { it.blockName == blockName } ?: return emptyList()
        val blockExercises = block.prescription.map { it.exercise }.toSet()

        val sessions = history.groupBy { it.sessionId }
        // Sort by timestamp descending
        val sortedSessions = sessions.values.sortedByDescending { it.maxOfOrNull { l -> l.timestamp } ?: 0L }

        return sortedSessions.filter { session ->
            session.any { it.exerciseName in blockExercises && !it.skipped }
        }
    }
}
