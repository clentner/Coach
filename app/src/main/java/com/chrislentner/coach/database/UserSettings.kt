package com.chrislentner.coach.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val maxHeartRate: Int? = null
)
