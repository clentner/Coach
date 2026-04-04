package com.chrislentner.coach.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScheduleEntry::class, WorkoutSession::class, WorkoutLogEntry::class, UserSettings::class], version = 10, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_logs ADD COLUMN customTargets TEXT")
                db.execSQL("ALTER TABLE workout_logs ADD COLUMN customFatigue TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`id` INTEGER NOT NULL, `maxHeartRate` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("DROP TABLE IF EXISTS `User`")
            }
        }

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coach-database"
                )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
