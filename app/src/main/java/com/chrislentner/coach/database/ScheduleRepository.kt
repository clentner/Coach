package com.chrislentner.coach.database

import android.content.Context
import androidx.room.Room

class ScheduleRepository(private val context: Context) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "coach-database"
        )
            .fallbackToDestructiveMigration() // Simple migration strategy for this task
            .build()
    }

    private val dao: ScheduleDao by lazy { db.scheduleDao() }

    suspend fun getScheduleByDate(date: String): ScheduleEntry? {
        return dao.getScheduleByDate(date)
    }

    suspend fun getLastSchedule(): ScheduleEntry? {
        return dao.getLastSchedule()
    }

    suspend fun saveSchedule(entry: ScheduleEntry) {
        dao.insertOrUpdate(entry)
    }
}
