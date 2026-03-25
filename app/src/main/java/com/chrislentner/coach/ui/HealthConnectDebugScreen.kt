package com.chrislentner.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chrislentner.coach.health.HealthConnectManager
import com.chrislentner.coach.health.FlattenedHrSample
import com.chrislentner.coach.health.ZoneResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.health.connect.client.HealthConnectClient
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectDebugScreen(
    navController: NavController,
    viewModel: HealthConnectDebugViewModel
) {
    val state by viewModel.state.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.checkStatusAndPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Connect Debug") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            HeaderSection(
                state = state,
                onRequestPermissions = {
                    requestPermissionLauncher.launch(HealthConnectManager.getRequiredPermissions())
                },
                onRefresh = { viewModel.checkStatusAndPermissions() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ZoneConfigSection(
                maxHrInput = state.maxHrInput,
                onMaxHrChange = { viewModel.updateMaxHr(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                if (state.isLoading) {
                    item { Text("Loading sessions...") }
                }
                items(sessions) { model ->
                    SessionDebugCard(
                        model = model,
                        maxHrInput = state.maxHrInput,
                        onToggleExpand = { viewModel.toggleSessionExpanded(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun SessionDebugCard(
    model: SessionDebugModel,
    maxHrInput: String,
    onToggleExpand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onToggleExpand(model.session.metadata.id) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val session = model.session
            val duration = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
            Text("Session: ${dtf.format(session.startTime)}", style = MaterialTheme.typography.titleMedium)
            Text("Type: ${session.exerciseType} | Duration: $duration min")
            Text("Source: ${session.metadata.dataOrigin.packageName}")

            // Warning badges
            val diag = model.diagnostics
            if (diag != null) {
                if (diag.hasNoHrRecords) {
                    Text("Badge: ⚠️ No HR Records", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (diag.hasNoHrSamples) {
                    Text("Badge: ⚠️ No Samples", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (session.metadata.dataOrigin.packageName != "com.garmin.android.apps.connectmobile") {
                    Text("Badge: ⚠️ Unknown Source", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                }
            } else if (!model.isLoadingHr && model.hrRecords == null && !model.isExpanded) {
                 // Not loaded yet, but if global permissions missing we could flag it here.
            }

            if (model.isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (model.isLoadingHr) {
                    Text("Loading HR data...")
                } else if (model.loadError != null) {
                    Text("Error: ${model.loadError}", color = MaterialTheme.colorScheme.error)
                } else {
                    SessionDetailSection(model = model, maxHrInput = maxHrInput)
                }
            } else {
                if (model.computedZones != null) {
                    Text("Samples: ${model.computedZones.sampleCount} | Avg HR: ${model.computedZones.avgHr ?: "N/A"}")
                }
            }
        }
    }
}

@Composable
fun SessionDetailSection(model: SessionDebugModel, maxHrInput: String) {
    val session = model.session
    val hrRecords = model.hrRecords ?: emptyList()
    val samples = model.hrSamples ?: emptyList()
    val zones = model.computedZones

    Text("--- Raw ExerciseSessionRecord ---", style = MaterialTheme.typography.labelMedium)
    Text("ID: ${session.metadata.id}", style = MaterialTheme.typography.bodySmall)
    Text("Title: ${session.title ?: "None"}", style = MaterialTheme.typography.bodySmall)
    Text("Start: ${dtf.format(session.startTime)}", style = MaterialTheme.typography.bodySmall)
    Text("End: ${dtf.format(session.endTime)}", style = MaterialTheme.typography.bodySmall)

    Spacer(modifier = Modifier.height(8.dp))
    Text("--- Raw HeartRateRecords ---", style = MaterialTheme.typography.labelMedium)
    Text("Total Records: ${hrRecords.size}")
    if (hrRecords.isEmpty()) {
        Text("No heart rate records found for this session.", color = MaterialTheme.colorScheme.error)
    } else {
        hrRecords.forEachIndexed { index, record ->
            Text("Record $index: ${record.samples.size} samples, source: ${record.metadata.dataOrigin.packageName}", style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("--- Flattened Samples Diagnostics ---", style = MaterialTheme.typography.labelMedium)
    val diag = model.diagnostics
    if (diag != null) {
        Text("Total Samples: ${diag.totalSamples}")
        if (diag.totalSamples > 0) {
            Text("First: ${diag.firstSampleTime?.let { dtf.format(it) }}")
            Text("Last: ${diag.lastSampleTime?.let { dtf.format(it) }}")
            Text("Gaps (s) -> Min: ${diag.minGapSecs} | Max: ${diag.maxGapSecs} | Median: ${diag.medianGapSecs}")

            if (diag.hasNoHrRecords) Text("⚠️ No HR Records found", color = MaterialTheme.colorScheme.error)
            if (diag.hasNoHrSamples) Text("⚠️ No HR Samples found", color = MaterialTheme.colorScheme.error)
            if (diag.hasDuplicateTimestamps) Text("⚠️ Duplicate timestamps detected", color = MaterialTheme.colorScheme.error)
            if (diag.isNonMonotonic) Text("⚠️ Non-monotonic (time reversed) detected", color = MaterialTheme.colorScheme.error)
            if (diag.hasSparseData) Text("⚠️ Sparse data detected (median gap > 15s)", color = MaterialTheme.colorScheme.error)
            if (diag.multipleSourcePackages) Text("⚠️ Multiple source packages in session", color = MaterialTheme.colorScheme.error)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("--- Granular Samples ---", style = MaterialTheme.typography.labelMedium)
    if (samples.isEmpty()) {
        Text("No samples to display.")
    } else {
        // Render a limited sample set or put it in a box. Since this is in a LazyColumn already, we shouldn't nest infinitely expanding lists easily, but we can just emit Column items since it's debug.
        // For performance if it's huge, maybe take first 100, but spec says "do not downsample".
        // We will just print them all, as it's a debug screen.
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
            // A pseudo-table
            LazyColumn {
                items(samples.size) { i ->
                    val s = samples[i]
                    val deltaStr = if (i > 0) {
                        "+${ChronoUnit.SECONDS.between(samples[i-1].time, s.time)}s"
                    } else {
                        "0s"
                    }
                    val pkgShort = s.sourcePackage?.split(".")?.lastOrNull() ?: "null"
                    val idShort = s.recordId.takeLast(6)
                    Text(
                        text = "${dtf.format(s.time)} | ${s.bpm} bpm | $deltaStr | id:..$idShort | pkg:$pkgShort",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("--- Computed Zone Results ---", style = MaterialTheme.typography.labelMedium)
    Text("Max HR Input Used: $maxHrInput")
    if (zones != null) {
        Text("Session Duration: ${zones.sessionDurationSecs}s")
        Text("Covered Duration: ${zones.coveredDurationSecs}s")
        Text("Uncovered Duration: ${maxOf(0, zones.sessionDurationSecs - zones.coveredDurationSecs)}s")
        Text("Tail Rule Applied: ${zones.tailRuleApplied ?: "None"}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(4.dp))
        val totalCovered = maxOf(1L, zones.coveredDurationSecs).toDouble()
        for (z in 5 downTo 1) {
            val d = zones.durationsPerZone[z] ?: 0L
            val pct = (d / totalCovered) * 100.0
            Text(String.format("Zone %d: %ds (%.1f%%)", z, d, pct))
        }
    } else {
        Text("Zone computation not available. Ensure Max HR is valid.")
    }
}

@Composable
fun HeaderSection(
    state: HealthConnectState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status", style = MaterialTheme.typography.titleMedium)

            val statusText = when (state.sdkStatus) {
                HealthConnectClient.SDK_AVAILABLE -> "Available"
                HealthConnectClient.SDK_UNAVAILABLE -> "Unavailable on this device"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Provider Update Required"
                else -> "Unknown (${state.sdkStatus})"
            }
            Text("Provider Status: $statusText")
            Text("Permissions Granted: ${state.hasPermissions}")

            if (state.error != null) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                    Button(onClick = {
                        context.startActivity(HealthConnectManager.getUpdateIntent(context))
                    }) {
                        Text("Update/Install Provider")
                    }
                } else if (state.isAvailable && !state.hasPermissions) {
                    Button(onClick = onRequestPermissions) {
                        Text("Request Permissions")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
fun ZoneConfigSection(
    maxHrInput: String,
    onMaxHrChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zone Configuration", style = MaterialTheme.typography.titleMedium)
            Text("Used only for zone computation on this debug screen.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = maxHrInput,
                onValueChange = {
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        onMaxHrChange(it)
                    }
                },
                label = { Text("Max HR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            val maxHr = maxHrInput.toIntOrNull()
            if (maxHr == null || maxHr !in 100..240) {
                Text("Please enter a valid Max HR between 100 and 240.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
