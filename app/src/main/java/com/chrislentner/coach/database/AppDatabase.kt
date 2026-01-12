package com.chrislentner.coach.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val uid: Int,
    val firstName: String?,
    val lastName: String?
)

@Database(entities = [User::class, ScheduleEntry::class, WorkoutSession::class, WorkoutLogEntry::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    // abstract fun userDao(): UserDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun workoutDao(): WorkoutDao
}
