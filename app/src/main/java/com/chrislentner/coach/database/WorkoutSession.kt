package com.chrislentner.coach.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val startTimeInMillis: Long,
    val endTimeInMillis: Long? = null,
    val isCompleted: Boolean = false,
    val location: String? = null
)
