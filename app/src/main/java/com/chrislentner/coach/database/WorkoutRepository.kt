package com.chrislentner.coach.database

// Normally we would use Dependency Injection (Hilt/Koin), but for this project we'll do manual DI or a singleton.
// Assuming the AppDatabase is provided elsewhere.
// But for now, let's just make a Repository class that wraps the Dao.

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    suspend fun getSessionById(id: Long): WorkoutSession? {
        return workoutDao.getSessionById(id)
    }

    suspend fun getOrCreateSession(date: String, timestamp: Long, location: String? = null): WorkoutSession {
        val existing = workoutDao.getSessionByDate(date)
        if (existing != null) return existing

        val newSession = WorkoutSession(
            date = date,
            startTimeInMillis = timestamp,
            isCompleted = false,
            location = location
        )
        val id = workoutDao.insertSession(newSession)
        return newSession.copy(id = id)
    }

    suspend fun getSessionsWithSetCounts(): List<SessionSummary> {
        return workoutDao.getSessionsWithSetCounts()
    }

    suspend fun getLogById(id: Long): WorkoutLogEntry? {
        return workoutDao.getLogById(id)
    }

    suspend fun logSet(entry: WorkoutLogEntry) {
        workoutDao.insertLogEntry(entry)
    }

    suspend fun updateLog(entry: WorkoutLogEntry) {
        workoutDao.updateLogEntry(entry)
    }

    suspend fun deleteLog(entry: WorkoutLogEntry) {
        workoutDao.deleteLogEntry(entry)
    }

    suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry> {
        return workoutDao.getLogsForSession(sessionId)
    }

    suspend fun getHistorySince(timestamp: Long): List<WorkoutLogEntry> {
        return workoutDao.getLogsSince(timestamp)
    }

    suspend fun getRecentExerciseNames(limit: Int): List<String> {
        return workoutDao.getRecentExerciseNames(limit)
    }

    suspend fun getAllExerciseNamesOrderedByRecency(): List<String> {
        // Fetch a large number to approximate "all" but keep it safe.
        // 1000 unique exercise names is a reasonable upper bound for a personal app.
        return workoutDao.getRecentExerciseNames(1000)
    }
}
