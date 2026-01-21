package com.chrislentner.coach.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SessionSummary(
    val id: Long,
    val date: String,
    val location: String?,
    val setCount: Int
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): WorkoutLogEntry?

    @Insert
    suspend fun insertLogEntry(entry: WorkoutLogEntry): Long

    @androidx.room.Update
    suspend fun updateLogEntry(entry: WorkoutLogEntry)

    @androidx.room.Delete
    suspend fun deleteLogEntry(entry: WorkoutLogEntry)

    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry>

    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSessionFlow(sessionId: Long): Flow<List<WorkoutLogEntry>>

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<WorkoutLogEntry>

    // For planner history
    @Query("SELECT * FROM workout_logs WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    suspend fun getLogsSince(timestamp: Long): List<WorkoutLogEntry>

    @Query("""
        SELECT
            s.id,
            s.date,
            s.location,
            COUNT(CASE WHEN l.skipped = 0 THEN 1 END) as setCount
        FROM workout_sessions s
        LEFT JOIN workout_logs l ON s.id = l.sessionId
        GROUP BY s.id
        ORDER BY s.date DESC
    """)
    suspend fun getSessionsWithSetCounts(): List<SessionSummary>

    @Query("""
        SELECT
            s.id,
            s.date,
            s.location,
            COUNT(CASE WHEN l.skipped = 0 THEN 1 END) as setCount
        FROM workout_sessions s
        LEFT JOIN workout_logs l ON s.id = l.sessionId
        GROUP BY s.id
        ORDER BY s.date DESC
    """)
    fun getSessionsWithSetCountsFlow(): Flow<List<SessionSummary>>

    @Query("SELECT exerciseName FROM workout_logs GROUP BY exerciseName ORDER BY MAX(timestamp) DESC LIMIT :limit")
    suspend fun getRecentExerciseNames(limit: Int): List<String>
}
