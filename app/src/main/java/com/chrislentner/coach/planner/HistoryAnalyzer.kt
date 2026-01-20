package com.chrislentner.coach.planner

import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.CoachConfig
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max

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
            block.contributesTo.forEach { contribution ->
                val exercises = block.prescription.map { it.exercise }.toSet()
                map.getOrPut(contribution.target) { mutableSetOf() }.addAll(exercises)
            }
        }
        return map
    }

    fun getAccumulatedFatigue(kind: String, windowHours: Int, now: Date, history: List<WorkoutLogEntry>): Double {
        val cutoff = now.time - TimeUnit.HOURS.toMillis(windowHours.toLong())
        var total = 0.0

        history.filter { it.timestamp in cutoff..now.time - 1 }.forEach { log ->
            // Note: If multiple exercises match, we use the first fatigue def found (via map, last write wins if duplicates).
            // Assuming unique exercise names across config or consistent fatigue loads.
            val fatigueDef = exerciseFatigueMap[log.exerciseName]
            if (fatigueDef != null && fatigueDef.containsKey(kind)) {
                val valOrFormula = fatigueDef[kind]
                val load = if (valOrFormula is Number) {
                    valOrFormula.toDouble()
                } else if (valOrFormula is String) {
                    val minutes = (log.actualDurationSeconds ?: 0) / 60.0
                    MathEvaluator.evaluate(valOrFormula, mapOf("performed_minutes" to minutes, "\$performed_minutes" to minutes))
                } else {
                    0.0
                }
                total += load
            }
        }
        return total
    }

    fun getDeficit(targetId: String, windowDays: Int, now: Date, history: List<WorkoutLogEntry>): Double {
         val targetConfig = config.targets.find { it.id == targetId } ?: return 0.0
         val cutoff = now.time - TimeUnit.DAYS.toMillis(windowDays.toLong())

         val contributingExercises = targetContributingExercises[targetId] ?: emptySet()

         var performed = 0.0
         history.filter { it.timestamp in cutoff..now.time && contributingExercises.contains(it.exerciseName) }
             .forEach { log ->
                 if (!log.skipped) {
                     if (targetConfig.type == "sets") {
                         // Assume one log entry = one set
                         performed += 1.0
                     } else if (targetConfig.type == "minutes") {
                         performed += (log.actualDurationSeconds ?: 0) / 60.0
                     }
                 }
             }

         return max(0.0, targetConfig.goal - performed)
    }

    fun isTargetMet(targetId: String, windowDays: Int, now: Date, history: List<WorkoutLogEntry>): Boolean {
        return getDeficit(targetId, windowDays, now, history) <= 0.0
    }

    fun getLastSatisfyingSessions(blockName: String, history: List<WorkoutLogEntry>): List<List<WorkoutLogEntry>> {
        val block = allBlocks.find { it.blockName == blockName } ?: return emptyList()

        val sessions = history.groupBy { it.sessionId }
        // Sort by timestamp descending
        val sortedSessions = sessions.values.sortedByDescending { it.maxOfOrNull { l -> l.timestamp } ?: 0L }

        return sortedSessions.filter { isBlockSatisfied(block, it) }
    }

    private fun isBlockSatisfied(block: Block, logs: List<WorkoutLogEntry>): Boolean {
        val blockExercises = block.prescription.map { it.exercise }.toSet()
        val matchingLogs = logs.filter { it.exerciseName in blockExercises }

        if (matchingLogs.isEmpty()) return false

        val prescribedSets = block.prescription.sumOf { it.sets ?: 0 }

        if (prescribedSets > 0) {
            val actualSets = matchingLogs.count { !it.skipped }
            return actualSets.toDouble() >= (prescribedSets * 0.5)
        } else {
             val totalDuration = matchingLogs.sumOf { it.actualDurationSeconds ?: 0 }
             return totalDuration > 0
        }
    }
}
