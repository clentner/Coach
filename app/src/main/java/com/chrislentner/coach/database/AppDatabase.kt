package com.chrislentner.coach.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity
data class User(
    @PrimaryKey val uid: Int,
    val firstName: String?,
    val lastName: String?
)

@Database(entities = [User::class, ScheduleEntry::class, WorkoutSession::class, WorkoutLogEntry::class], version = 9, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    // abstract fun userDao(): UserDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_logs ADD COLUMN customTargets TEXT")
                db.execSQL("ALTER TABLE workout_logs ADD COLUMN customFatigue TEXT")
            }
        }

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coach-database"
                )
                    .addMigrations(MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
