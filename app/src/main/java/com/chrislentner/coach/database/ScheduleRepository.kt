package com.chrislentner.coach.database

class ScheduleRepository(private val dao: ScheduleDao) {

    suspend fun getScheduleByDate(date: String): ScheduleEntry? {
        return dao.getScheduleByDate(date)
    }

    suspend fun getScheduleBetweenDates(startDate: String, endDate: String): List<ScheduleEntry> {
        return dao.getScheduleBetweenDates(startDate, endDate)
    }

    suspend fun getLastSchedule(): ScheduleEntry? {
        return dao.getLastSchedule()
    }

    suspend fun saveSchedule(entry: ScheduleEntry) {
        dao.insertOrUpdate(entry)
    }
}
