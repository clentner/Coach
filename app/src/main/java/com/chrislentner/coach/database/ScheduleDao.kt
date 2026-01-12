package com.chrislentner.coach.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_entries WHERE date = :date")
    suspend fun getScheduleByDate(date: String): ScheduleEntry?

    @Query("SELECT * FROM schedule_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLastSchedule(): ScheduleEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: ScheduleEntry)
}
