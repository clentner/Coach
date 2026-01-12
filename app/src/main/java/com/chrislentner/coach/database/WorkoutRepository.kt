package com.chrislentner.coach.database

import android.content.Context
import androidx.room.Room

class WorkoutRepository(private val context: Context) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "coach-database"
        )
            .fallbackToDestructiveMigration() // Simple migration strategy for this task
            .build()
    }

    private val dao: WorkoutDao by lazy { db.workoutDao() }

    suspend fun getWorkoutByDate(date: String): WorkoutEntry? {
        return dao.getWorkoutByDate(date)
    }

    suspend fun getLastWorkout(): WorkoutEntry? {
        return dao.getLastWorkout()
    }

    suspend fun saveWorkout(entry: WorkoutEntry) {
        dao.insertOrUpdate(entry)
    }
}
