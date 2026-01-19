package com.chrislentner.coach.planner.model

import java.util.Date

data class Schedule(
    val sessions: List<ScheduledSession>
)

data class ScheduledSession(
    val date: Date,
    val location: String,
    val blocks: List<Block>,
    val durationMinutes: Int
)
