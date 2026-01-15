package com.chrislentner.coach.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class WorkoutLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val targetReps: Int?,
    val targetDurationSeconds: Int?,
    val loadDescription: String, // e.g., "85 lbs" or "Blue Band"
    val tempo: String? = null,
    val actualReps: Int?,
    val actualDurationSeconds: Int?,
    val rpe: Int?, // 1-10
    val notes: String?,
    val skipped: Boolean = false,
    val timestamp: Long
) {
    init {
        if (tempo != null) {
            require(tempo.matches(Regex("\\d{4}"))) { "Tempo must be a 4-digit string (e.g., '3030')" }
        }
    }
}
