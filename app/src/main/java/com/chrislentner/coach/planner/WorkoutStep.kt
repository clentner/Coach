package com.chrislentner.coach.planner

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
