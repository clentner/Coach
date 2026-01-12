package com.chrislentner.coach.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_entries WHERE date = :date")
    suspend fun getWorkoutByDate(date: String): WorkoutEntry?

    @Query("SELECT * FROM workout_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLastWorkout(): WorkoutEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WorkoutEntry)
}
