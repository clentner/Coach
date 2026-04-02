package com.chrislentner.coach.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserSettingsRepository(private val dao: UserSettingsDao) {
    suspend fun getUserSettings(): UserSettings? = withContext(Dispatchers.IO) {
        dao.getUserSettings()
    }

    suspend fun saveUserSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(settings)
    }
}
