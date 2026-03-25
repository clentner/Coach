package com.chrislentner.coach.health

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

data class FlattenedHrSample(
    val time: Instant,
    val bpm: Long,
    val recordId: String,
    val sourcePackage: String?
)

data class RawDiagnostics(
    val totalSamples: Int,
    val firstSampleTime: Instant?,
    val lastSampleTime: Instant?,
    val minGapSecs: Long,
    val maxGapSecs: Long,
    val medianGapSecs: Long,
    val hasNoHrRecords: Boolean,
    val hasNoHrSamples: Boolean,
    val hasDuplicateTimestamps: Boolean,
    val isNonMonotonic: Boolean,
    val hasSparseData: Boolean,
    val multipleSourcePackages: Boolean
)

data class ZoneResult(
    val durationsPerZone: Map<Int, Long>, // 1..5 mapped to duration in seconds
    val sampleCount: Int,
    val sessionDurationSecs: Long,
    val coveredDurationSecs: Long,
    val minHr: Long?,
    val maxObservedHr: Long?,
    val avgHr: Long?,
    val tailRuleApplied: String?
)

object HealthConnectManager {

    fun getAvailabilityStatus(context: Context): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    fun isAvailable(context: Context): Boolean {
        return getAvailabilityStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun getClient(context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )
    }

    fun hasAllPermissions(context: Context): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun checkPermissionsGranted(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = getClient(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(getRequiredPermissions())
    }

    suspend fun readExerciseSessions(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): List<ExerciseSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val response = client.readRecords(request)
        // Ensure sorted descending
        return response.records.sortedByDescending { it.startTime }
    }

    suspend fun readHeartRateRecords(
        client: HealthConnectClient,
        session: ExerciseSessionRecord
    ): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
        )
        val response = client.readRecords(request)
        return response.records
    }

    fun flattenHeartRateSamples(records: List<HeartRateRecord>): List<FlattenedHrSample> {
        return records.flatMap { record ->
            record.samples.map { sample ->
                FlattenedHrSample(
                    time = sample.time,
                    bpm = sample.beatsPerMinute,
                    recordId = record.metadata.id,
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        }.sortedBy { it.time }
    }

    fun computeDiagnostics(
        records: List<HeartRateRecord>,
        flattened: List<FlattenedHrSample>
    ): RawDiagnostics {
        if (flattened.isEmpty()) {
            return RawDiagnostics(
                totalSamples = 0,
                firstSampleTime = null,
                lastSampleTime = null,
                minGapSecs = 0,
                maxGapSecs = 0,
                medianGapSecs = 0,
                hasNoHrRecords = records.isEmpty(),
                hasNoHrSamples = true,
                hasDuplicateTimestamps = false,
                isNonMonotonic = false,
                hasSparseData = false,
                multipleSourcePackages = false
            )
        }

        val first = flattened.first().time
        val last = flattened.last().time

        var minGap = Long.MAX_VALUE
        var maxGap = 0L
        val gaps = mutableListOf<Long>()
        var hasDups = false
        var isNonMonotonic = false

        // Note: the input `flattened` is already sorted by time from `flattenHeartRateSamples`
        // so `isNonMonotonic` check against original order would require un-sorted data.
        // We will just check if time goes backwards which it shouldn't if sorted, but we check anyway.
        for (i in 0 until flattened.size - 1) {
            val gap = ChronoUnit.SECONDS.between(flattened[i].time, flattened[i+1].time)
            if (gap < 0) {
                isNonMonotonic = true
            } else {
                gaps.add(gap)
                if (gap < minGap) minGap = gap
                if (gap > maxGap) maxGap = gap
                if (gap == 0L) hasDups = true
            }
        }

        if (minGap == Long.MAX_VALUE) minGap = 0

        val medianGap = if (gaps.isNotEmpty()) {
            gaps.sort()
            if (gaps.size % 2 == 0) {
                (gaps[gaps.size / 2 - 1] + gaps[gaps.size / 2]) / 2
            } else {
                gaps[gaps.size / 2]
            }
        } else {
            0L
        }

        val sources = flattened.mapNotNull { it.sourcePackage }.toSet()
        val multipleSources = sources.size > 1

        val hasSparseData = medianGap > 15L // Implementation defined threshold

        return RawDiagnostics(
            totalSamples = flattened.size,
            firstSampleTime = first,
            lastSampleTime = last,
            minGapSecs = minGap,
            maxGapSecs = maxGap,
            medianGapSecs = medianGap,
            hasNoHrRecords = records.isEmpty(),
            hasNoHrSamples = false,
            hasDuplicateTimestamps = hasDups,
            isNonMonotonic = isNonMonotonic,
            hasSparseData = hasSparseData,
            multipleSourcePackages = multipleSources
        )
    }

    fun computeHeartRateZones(
        session: ExerciseSessionRecord,
        flattened: List<FlattenedHrSample>,
        maxHr: Int
    ): ZoneResult {
        val sessionDuration = ChronoUnit.SECONDS.between(session.startTime, session.endTime)

        if (flattened.isEmpty()) {
            return ZoneResult(
                emptyMap(), 0, sessionDuration, 0, null, null, null, null
            )
        }

        // Drop exact duplicates
        val deduplicated = mutableListOf<FlattenedHrSample>()
        for (sample in flattened) {
            val last = deduplicated.lastOrNull()
            if (last != null && last.time == sample.time && last.bpm == sample.bpm) {
                // identical, skip
                continue
            }
            deduplicated.add(sample)
        }

        val minHr = deduplicated.minOf { it.bpm }
        val maxObsHr = deduplicated.maxOf { it.bpm }
        val avgHr = deduplicated.sumOf { it.bpm } / deduplicated.size

        val zoneDurations = mutableMapOf<Int, Long>(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
        var coveredTime = 0L
        var tailRule: String? = null

        for (i in deduplicated.indices) {
            val sample = deduplicated[i]
            val pct = sample.bpm.toDouble() / maxHr.toDouble()
            val zone = when {
                pct >= 0.9 -> 5
                pct >= 0.8 -> 4
                pct >= 0.7 -> 3
                pct >= 0.6 -> 2
                pct >= 0.5 -> 1
                else -> 0 // below zone 1, not tracked or could track as 0
            }

            var duration = 0L
            if (i < deduplicated.size - 1) {
                val next = deduplicated[i + 1]
                duration = ChronoUnit.SECONDS.between(sample.time, next.time)
                // clamp negative or absurd
                if (duration < 0) duration = 0L
            } else {
                // tail rule for last sample
                if (i > 0) {
                    val prev = deduplicated[i - 1]
                    val prevGap = ChronoUnit.SECONDS.between(prev.time, sample.time)
                    val safeGap = if (prevGap < 0) 0L else prevGap
                    duration = minOf(safeGap, 5L)
                    tailRule = "min(previousGap ($safeGap), 5s) -> ${duration}s"
                } else {
                    duration = 0L
                    tailRule = "no previous gap -> 0s"
                }
            }

            if (zone in 1..5) {
                val currentDur = zoneDurations[zone] ?: 0L
                zoneDurations[zone] = currentDur + duration
            }
            coveredTime += duration
        }

        val result = ZoneResult(
            durationsPerZone = zoneDurations,
            sampleCount = deduplicated.size,
            sessionDurationSecs = sessionDuration,
            coveredDurationSecs = coveredTime,
            minHr = minHr,
            maxObservedHr = maxObsHr,
            avgHr = avgHr,
            tailRuleApplied = tailRule
        )

        return result
    }
}