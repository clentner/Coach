package com.chrislentner.coach.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

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

    @Insert
    suspend fun insertLogEntry(entry: WorkoutLogEntry): Long

    @androidx.room.Delete
    suspend fun deleteLogEntry(entry: WorkoutLogEntry)

    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogsForSession(sessionId: Long): List<WorkoutLogEntry>

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
            COUNT(l.id) as setCount
        FROM workout_sessions s
        LEFT JOIN workout_logs l ON s.id = l.sessionId
        GROUP BY s.id
        ORDER BY s.date DESC
    """)
    suspend fun getSessionsWithSetCounts(): List<SessionSummary>
}
