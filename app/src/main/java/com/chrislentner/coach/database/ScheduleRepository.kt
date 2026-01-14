package com.chrislentner.coach.database

class ScheduleRepository(private val dao: ScheduleDao) {

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
