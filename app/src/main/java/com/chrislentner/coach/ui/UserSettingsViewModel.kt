package com.chrislentner.coach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.database.UserSettings
import com.chrislentner.coach.database.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserSettingsViewModel(private val repository: UserSettingsRepository) : ViewModel() {

    private val _maxHeartRate = MutableStateFlow("")
    val maxHeartRate: StateFlow<String> = _maxHeartRate.asStateFlow()

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = repository.getUserSettings()
            if (settings?.maxHeartRate != null) {
                _maxHeartRate.value = settings.maxHeartRate.toString()
            }
        }
    }

    fun updateMaxHeartRate(newValue: String) {
        _maxHeartRate.value = newValue
        _saveStatus.value = null // clear status on edit
    }

    fun saveSettings() {
        val rateInt = _maxHeartRate.value.toIntOrNull()
        if (_maxHeartRate.value.isNotEmpty() && rateInt == null) {
             _saveStatus.value = "Invalid max heart rate."
             return
        }

        viewModelScope.launch {
            repository.saveUserSettings(UserSettings(maxHeartRate = rateInt))
            _saveStatus.value = "Settings saved successfully."
        }
    }
}

class UserSettingsViewModelFactory(
    private val repository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
