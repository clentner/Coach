package com.chrislentner.coach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.chrislentner.coach.health.FlattenedHrSample
import androidx.lifecycle.viewModelScope
import com.chrislentner.coach.health.HealthConnectManager
import com.chrislentner.coach.health.RawDiagnostics
import com.chrislentner.coach.health.ZoneResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.chrislentner.coach.health.HealthConnectSyncRoutine
import com.chrislentner.coach.database.AppDatabase
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.database.UserSettingsRepository

data class HealthConnectState(
    val isAvailable: Boolean = false,
    val sdkStatus: Int = 2, // Default SDK_UNAVAILABLE
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val maxHrInput: String = "180", // Default input as string for text field
    val syncLog: String = "",
    val isSyncing: Boolean = false
)

data class SessionDebugModel(
    val session: ExerciseSessionRecord,
    val isLoadingHr: Boolean = false,
    val hrRecords: List<HeartRateRecord>? = null,
    val hrSamples: List<FlattenedHrSample>? = null,
    val diagnostics: RawDiagnostics? = null,
    val computedZones: ZoneResult? = null,
    val loadError: String? = null,
    val isExpanded: Boolean = false
)

class HealthConnectDebugViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(HealthConnectState())
    val state: StateFlow<HealthConnectState> = _state.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionDebugModel>>(emptyList())
    val sessions: StateFlow<List<SessionDebugModel>> = _sessions.asStateFlow()

    init {
        checkStatusAndPermissions()
    }

    fun checkStatusAndPermissions() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val status = HealthConnectManager.getAvailabilityStatus(context)
            val isAvail = HealthConnectManager.isAvailable(context)
            val hasPerms = if (isAvail) {
                HealthConnectManager.checkPermissionsGranted(context)
            } else {
                false
            }

            _state.update {
                it.copy(
                    isAvailable = isAvail,
                    sdkStatus = status,
                    hasPermissions = hasPerms
                )
            }

            if (isAvail && hasPerms) {
                loadSessions()
            }
        }
    }

    fun loadSessions() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val client = HealthConnectManager.getClient(context)
                val now = Instant.now()
                val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

                val records = HealthConnectManager.readExerciseSessions(client, thirtyDaysAgo, now)

                _sessions.value = records.map {
                    SessionDebugModel(session = it)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load sessions: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateMaxHr(input: String) {
        _state.update { it.copy(maxHrInput = input) }
        // Recompute visible sessions if valid
        val validHr = input.toIntOrNull()
        val currentSessions = _sessions.value
        _sessions.value = currentSessions.map { model ->
            if (model.hrSamples != null) {
                if (validHr != null && validHr in 100..240) {
                    val result = HealthConnectManager.computeHeartRateZones(
                        model.session, model.hrSamples, validHr
                    )
                    model.copy(computedZones = result)
                } else {
                    model.copy(computedZones = null)
                }
            } else {
                model
            }
        }
    }

    fun toggleSessionExpanded(sessionRecordId: String) {
        val currentSessions = _sessions.value
        val index = currentSessions.indexOfFirst { it.session.metadata.id == sessionRecordId }
        if (index == -1) return

        val model = currentSessions[index]
        val newExpanded = !model.isExpanded

        _sessions.value = currentSessions.toMutableList().apply {
            set(index, model.copy(isExpanded = newExpanded))
        }

        if (newExpanded && model.hrRecords == null && !model.isLoadingHr) {
            loadHeartRateForSession(sessionRecordId)
        }
    }

    private fun loadHeartRateForSession(sessionRecordId: String) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            // Update loading state
            updateSessionModel(sessionRecordId) { it.copy(isLoadingHr = true, loadError = null) }

            try {
                val client = HealthConnectManager.getClient(context)
                val currentSessions = _sessions.value
                val model = currentSessions.firstOrNull { it.session.metadata.id == sessionRecordId } ?: return@launch

                val records = HealthConnectManager.readHeartRateRecords(client, model.session)
                val samples = HealthConnectManager.flattenHeartRateSamples(records)
                val diagnostics = HealthConnectManager.computeDiagnostics(records, samples)

                val validHr = _state.value.maxHrInput.toIntOrNull()
                val result = if (validHr != null && validHr in 100..240) {
                    HealthConnectManager.computeHeartRateZones(model.session, samples, validHr)
                } else {
                    null
                }

                updateSessionModel(sessionRecordId) {
                    it.copy(
                        isLoadingHr = false,
                        hrRecords = records,
                        hrSamples = samples,
                        diagnostics = diagnostics,
                        computedZones = result
                    )
                }
            } catch (e: Exception) {
                updateSessionModel(sessionRecordId) {
                    it.copy(isLoadingHr = false, loadError = "Failed to load HR: ${e.message}")
                }
            }
        }
    }

    private fun updateSessionModel(id: String, mutator: (SessionDebugModel) -> SessionDebugModel) {
        val currentSessions = _sessions.value
        val index = currentSessions.indexOfFirst { it.session.metadata.id == id }
        if (index != -1) {
            _sessions.value = currentSessions.toMutableList().apply {
                set(index, mutator(this[index]))
            }
        }
    }

    fun runSync() {
        if (_state.value.isSyncing) return
        val context = getApplication<Application>()
        _state.update { it.copy(isSyncing = true, syncLog = "") }

        viewModelScope.launch {
            try {
                val client = HealthConnectManager.getClient(context)
                val db = AppDatabase.getDatabase(context)
                val workoutRepo = WorkoutRepository(db.workoutDao())
                val userSettingsRepo = UserSettingsRepository(db.userSettingsDao())

                val routine = HealthConnectSyncRoutine(context, client, workoutRepo, userSettingsRepo)
                routine.runSync().collect { logMsg ->
                    _state.update { it.copy(syncLog = it.syncLog + logMsg + "\n") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(syncLog = it.syncLog + "Sync Failed: ${e.message}\n") }
            } finally {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }
}
