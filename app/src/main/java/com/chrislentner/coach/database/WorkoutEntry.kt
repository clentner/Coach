package com.chrislentner.coach.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_entries")
data class WorkoutEntry(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val timeInMillis: Long,
    val durationMinutes: Int,
    val location: String // "Home" or "Gym"
)
