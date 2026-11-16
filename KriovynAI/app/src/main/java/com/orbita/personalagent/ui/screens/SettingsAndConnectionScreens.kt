package com.orbita.personalagent.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbita.personalagent.ai.ConnectionCheckResult
import com.orbita.personalagent.data.preferences.AiBackendMode
import com.orbita.personalagent.data.preferences.KriovynThemeMode
import com.orbita.personalagent.ui.components.ConnectionStatusBadge
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import com.orbita.personalagent.ui.theme.KriovynSuccess
import com.orbita.personalagent.ui.theme.KriovynWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenConnectionStatus: () -> Unit,
    onBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val banner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(banner) {
        banner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissBanner()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings & Privacy") }
            )
        }
    ) { innerPadding ->
        ResponsiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    KriovynCard(
                        modifier = Modifier.clickable(onClick = onOpenConnectionStatus),
                        accentBorder = KriovynBlueAccent.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Connection Status", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prefs.aiBackendMode.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ConnectionStatusBadge(
                                isDemoMode = prefs.aiBackendMode == AiBackendMode.DEMO_OFFLINE,
                                onClick = onOpenConnectionStatus
                            )
                        }
                    }
                }

                item {
                    KriovynCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = KriovynBlueAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Appearance Theme", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KriovynThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.displayName) }
                                )
                            }
                        }
                    }
                }

                item {
                    KriovynCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = KriovynPurpleAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Data & Agent Controls", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Propose Memory Suggestions", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Show confirmation cards when you say \"Remember...\". Never saves silently.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.memorySuggestionsEnabled,
                                onCheckedChange = viewModel::setMemorySuggestionsEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Tool Execution Summaries", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Display compact tool status badges beneath assistant replies.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.showToolStepDetails,
                                onCheckedChange = viewModel::setShowToolStepDetails
                            )
                        }
                    }
                }

                item {
                    KriovynCard(modifier = Modifier.clickable { showPrivacyDialog = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = KriovynSuccess)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Privacy & Security Architecture", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap to inspect Kriovyn AI's local storage, intent safety, and zero-key guarantees.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    KriovynCard(accentBorder = KriovynError.copy(alpha = 0.6f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = KriovynError)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear All Local Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KriovynError
                                )
                                Text(
                                    text = "Erases all chats, tasks, notes, and approved memories stored on this device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showClearDataConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Erase Local Database", color = KriovynError, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All Local Data?") },
            text = {
                Text("This permanently deletes all conversation sessions, tasks, notes, and approved personal memories. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLocalData()
                        showClearDataConfirm = false
                    }
                ) {
                    Text("Erase Everything", color = KriovynError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Kriovyn AI Privacy Guarantees") },
            text = {
                Text(
                    "1. No API keys or credentials are compiled into this APK.\n" +
                        "2. Offline Demo Mode performs zero network traffic.\n" +
                        "3. Personal memories are only stored after explicit user approval.\n" +
                        "4. External Android actions (Dialer, SMS, Email, Calendar, Alarm, Maps, Web, Share) use standard Android intents and always require user approval first."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Got It") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConnectionStatusScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val checkResult by viewModel.connectionCheck.collectAsStateWithLifecycle()
    val isChecking by viewModel.isCheckingConnection.collectAsStateWithLifecycle()

    var endpointDraft by remember(prefs.backendEndpointUrl) {
        mutableStateOf(prefs.backendEndpointUrl)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("AI Connection Status") }
            )
        }
    ) { innerPadding ->
        ResponsiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val isDemo = prefs.aiBackendMode == AiBackendMode.DEMO_OFFLINE
                    KriovynCard(accentBorder = if (isDemo) KriovynWarning else KriovynSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDemo) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = if (isDemo) KriovynWarning else KriovynSuccess
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDemo) "Demo Mode Active (Offline)" else "Secure Backend Adapter",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDemo) KriovynWarning else KriovynSuccess
                                )
                                Text(
                                    text = if (isDemo) {
                                        "Clearly labeled offline test planner. All 12 Phase 1 tools and memory flows work locally without network access."
                                    } else {
                                        "Routes requests to your user-configured HTTPS proxy backend (no API keys stored in APK)."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    KriovynCard {
                        Text("Select AI Provider Mode", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        AiBackendMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAiBackendMode(mode) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = prefs.aiBackendMode == mode,
                                    onClick = { viewModel.setAiBackendMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = when (mode) {
                                            AiBackendMode.DEMO_OFFLINE -> "Deterministic local planner for Phase 1 testing."
                                            AiBackendMode.SECURE_BACKEND -> "HTTPS backend proxy adapter for Gemini server integration."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (prefs.aiBackendMode == AiBackendMode.SECURE_BACKEND) {
                    item {
                        KriovynCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = KriovynBlueAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Secure Backend Proxy URL", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "For security, Gemini API keys must stay on your backend server, never in the Android app.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = endpointDraft,
                                onValueChange = { endpointDraft = it },
                                label = { Text("HTTPS Endpoint URL") },
                                placeholder = { Text("https://api.example.com/kriovyn/agent") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.saveBackendEndpointUrl(endpointDraft) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KriovynBlueAccent,
                                    contentColor = KriovynNavyBlack
                                )
                            ) {
                                Text("Save & Verify Endpoint")
                            }
                        }
                    }
                }

                item {
                    KriovynCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Connection Diagnostics", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(
                                enabled = !isChecking,
                                onClick = { viewModel.runConnectionDiagnostic() }
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Test")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        when (val result = checkResult) {
                            is ConnectionCheckResult.Ready -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = KriovynSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${result.providerName} • ${result.latencyMs}ms",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = KriovynSuccess
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.detail, style = MaterialTheme.typography.bodyMedium)
                            }
                            is ConnectionCheckResult.Unavailable -> {
                                Text(
                                    text = "${result.providerName} Unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KriovynError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.reason, style = MaterialTheme.typography.bodyMedium)
                            }
                            null -> Text("Running initial diagnostics...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
