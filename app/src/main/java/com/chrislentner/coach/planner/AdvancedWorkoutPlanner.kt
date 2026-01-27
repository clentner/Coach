package com.chrislentner.coach.planner

import com.chrislentner.coach.database.ScheduleEntry
import com.chrislentner.coach.database.WorkoutLogEntry
import com.chrislentner.coach.planner.model.Block
import com.chrislentner.coach.planner.model.CoachConfig
import java.util.Date
import kotlin.math.max

data class BlockExecution(
    val blockName: String,
    val reduction: Double
)

data class Plan(
    val steps: List<WorkoutStep>,
    val logs: List<WorkoutLogEntry>,
    val blocks: List<BlockExecution>
)

class AdvancedWorkoutPlanner(
    val config: CoachConfig,
    private val historyAnalyzer: HistoryAnalyzer,
    private val progressionEngine: ProgressionEngine
) {

    private data class PlannedBlock(
        val block: Block,
        val effectiveSizeMinutes: Int,
        val steps: List<WorkoutStep>,
        val dummyLogs: List<WorkoutLogEntry>
    )

    fun generatePlan(
        today: Date,
        history: List<WorkoutLogEntry>,
        schedule: ScheduleEntry
    ): Plan {
        val plannedBlocks = mutableListOf<PlannedBlock>()
        val executedBlocks = mutableListOf<BlockExecution>()
        var timeRemaining = schedule.durationMinutes ?: 60

        // Initial Deficits
        val deficits = config.targets.associate { target ->
            target.id to historyAnalyzer.getDeficit(target.id, target.windowDays, today, history)
        }.toMutableMap()

        // Loop until no more blocks can be added
        while (true) {
            val bestBlock = findBestBlock(
                timeRemaining,
                schedule.location ?: "Home",
                deficits,
                history,
                plannedBlocks,
                today
            )

            if (bestBlock != null) {
                plannedBlocks.add(bestBlock)
                timeRemaining -= bestBlock.effectiveSizeMinutes

                // Update deficits
                var totalReduction = 0.0
                bestBlock.block.contributesTo.forEach { contribution ->
                    val currentDeficit = deficits[contribution.target] ?: 0.0
                    val reduction = calculateReduction(bestBlock, contribution.target)
                    val newDeficit = max(0.0, currentDeficit - reduction)
                    totalReduction += (currentDeficit - newDeficit)
                    deficits[contribution.target] = newDeficit
                }
                executedBlocks.add(BlockExecution(bestBlock.block.blockName, totalReduction))
            } else {
                break
            }
        }

        return Plan(
            steps = plannedBlocks.flatMap { it.steps },
            logs = plannedBlocks.flatMap { it.dummyLogs },
            blocks = executedBlocks
        )
    }

    private fun findBestBlock(
        timeRemaining: Int,
        location: String,
        deficits: Map<String, Double>,
        history: List<WorkoutLogEntry>,
        plannedBlocks: List<PlannedBlock>,
        today: Date
    ): PlannedBlock? {
        val plannedExercises = plannedBlocks.flatMap { it.steps.map { step -> step.exerciseName } }.toSet()

        for (priorityGroupKey in config.priorityOrder) {
            val group = config.priorities[priorityGroupKey]
                ?: throw IllegalStateException("Priority group '$priorityGroupKey' defined in priority_order but not found in priorities.")

            for (block in group.blocks) {
                if (block.location != "anywhere" && !block.location.equals(location, ignoreCase = true)) continue

                val blockExercises = block.prescription.map { it.exercise }.toSet()
                if (blockExercises.any { it in plannedExercises }) continue

                val helpsDeficit = block.contributesTo.any { (deficits[it.target] ?: 0.0) > 0.0 }
                if (!helpsDeficit) continue

                val progressionResult = progressionEngine.determineProgression(block, history)

                val possibleSizes = if (progressionResult.sizeMinutes != null) {
                    listOf(progressionResult.sizeMinutes)
                } else {
                    block.sizeMinutes
                }

                val fittingSizes = possibleSizes.filter { it <= timeRemaining }
                if (fittingSizes.isEmpty()) continue

                val selectedSize = selectSize(fittingSizes, block, deficits)

                val candidate = createPlannedBlock(block, selectedSize, progressionResult, today.time)

                if (checkFatigue(candidate, history, plannedBlocks, today)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun selectSize(fittingSizes: List<Int>, block: Block, deficits: Map<String, Double>): Int {
        val contributingTargets = block.contributesTo.mapNotNull { contrib ->
            config.targets.find { it.id == contrib.target }
        }

        val minutesTargets = contributingTargets.filter { it.type == "minutes" }

        if (minutesTargets.isNotEmpty()) {
            val maxDeficit = minutesTargets.maxOfOrNull { deficits[it.id] ?: 0.0 } ?: 0.0

            val necessarySizes = fittingSizes.filter { it.toDouble() <= maxDeficit }

            if (necessarySizes.isNotEmpty()) {
                return necessarySizes.max()
            } else {
                return fittingSizes.min()
            }
        } else {
            return fittingSizes.max()
        }
    }

    private fun checkFatigue(
        candidate: PlannedBlock,
        history: List<WorkoutLogEntry>,
        plannedBlocks: List<PlannedBlock>,
        today: Date
    ): Boolean {
        val priorHistory = history + plannedBlocks.flatMap { it.dummyLogs }
        val combinedHistory = priorHistory + candidate.dummyLogs

        val candidateTags = candidate.block.tags.toSet()

        config.fatigueConstraints.forEach { (kind, constraints) ->
            constraints.forEach { constraint ->
                val applies = constraint.appliesToBlocksWithTag.any { it in candidateTags }
                if (applies) {
                    val historyToUse = if (constraint.kind == "prior_load_lt") priorHistory else combinedHistory
                    val currentLoad = historyAnalyzer.getAccumulatedFatigue(kind, constraint.windowHours, today, historyToUse)
                    if (currentLoad >= constraint.threshold) {
                         return false
                    }
                }
            }
        }
        return true
    }

    private fun createPlannedBlock(
        block: Block,
        sizeMinutes: Int,
        progression: ProgressionResult,
        timestamp: Long
    ): PlannedBlock {
        val steps = block.prescription.flatMap { prescription ->
            val setCount = prescription.sets ?: 1
            List(setCount) {
                val loadDesc = if (progression.loadDescription != null) {
                    progression.loadDescription
                } else if (prescription.rpe != null) {
                    "RPE ${prescription.rpe}"
                } else if (prescription.target != null) {
                    "Target: ${prescription.target}"
                } else {
                    "Bodyweight"
                }

                WorkoutStep(
                    exerciseName = prescription.exercise,
                    targetReps = prescription.reps ?: (if (prescription.seconds != null) 0 else null),
                    targetDurationSeconds = prescription.seconds,
                    loadDescription = loadDesc,
                    tempo = prescription.tempo
                )
            }
        }

        val dummyLogs = steps.map { step ->
            WorkoutLogEntry(
                sessionId = -1,
                exerciseName = step.exerciseName,
                targetReps = step.targetReps,
                targetDurationSeconds = step.targetDurationSeconds,
                loadDescription = step.loadDescription,
                tempo = step.tempo,
                actualReps = step.targetReps,
                actualDurationSeconds = (sizeMinutes * 60) / steps.size,
                rpe = null,
                notes = "Planned",
                timestamp = timestamp
            )
        }

        return PlannedBlock(block, sizeMinutes, steps, dummyLogs)
    }

    private fun calculateReduction(plannedBlock: PlannedBlock, targetId: String): Double {
         val targetConfig = config.targets.find { it.id == targetId } ?: return 0.0
         if (targetConfig.type == "minutes") {
             return plannedBlock.effectiveSizeMinutes.toDouble()
         } else {
             return plannedBlock.steps.size.toDouble()
         }
    }
}
