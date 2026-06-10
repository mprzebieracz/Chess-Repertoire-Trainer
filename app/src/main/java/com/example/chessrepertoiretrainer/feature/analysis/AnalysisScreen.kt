package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.bluetooth.BluetoothAnalysisSession
import com.example.chessrepertoiretrainer.core.bluetooth.PairedDeviceUi
import com.example.chessrepertoiretrainer.core.bluetooth.rememberBluetoothConnectPermissionLauncher
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.chess.ui.DeeperButton
import com.example.chessrepertoiretrainer.core.chess.ui.EngineSection
import com.example.chessrepertoiretrainer.core.chess.ui.EngineToggleButton
import com.example.chessrepertoiretrainer.core.chess.ui.PgnTextViewer
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel, onBackClick: () -> Unit) {
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController

    var showBluetoothDialog by remember { mutableStateOf(false) }

    val requestPermissionForHost = rememberBluetoothConnectPermissionLauncher { granted ->
        if (granted) viewModel.hostBluetoothSession()
    }
    val requestPermissionForRefresh = rememberBluetoothConnectPermissionLauncher { granted ->
        if (granted) viewModel.refreshPairedDevices()
    }

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        annotations = viewModel.annotations,
        topBar = {
            ChessTopBar(
                title = "Analysis",
                onBackClick = onBackClick,
                actions = {
                    BluetoothButton(
                        state = bluetoothState,
                        onClick = { showBluetoothDialog = true })
                    engineAnalysis?.depth?.let { depth ->
                        if (isEngineEnabled) DeeperButton(
                            searchState = engineSearchState,
                            depth = depth,
                            onClick = viewModel::analyzeDeeper
                        )
                    }
                    EngineToggleButton(
                        isEnabled = isEngineEnabled,
                        onClick = viewModel::toggleEngine
                    )
                },
            )
        },
        engineSection = {
            if (!engineError.isNullOrBlank()) {
                Text(
                    text = engineError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            if (isEngineEnabled) {
                EngineSection(
                    analysis = engineAnalysis,
                    searchState = engineSearchState,
                    isFlipped = chessCtrl.isFlipped,
                )
            }
        },
        contentBar = {
            if (bluetoothState !is BluetoothAnalysisSession.State.Connected) {
                PgnTextViewer(
                    navigator = viewModel.navigator,
                    onMoveClick = { viewModel.navigator.goTo(it) },
                )
            }
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(AppIcons.FlipBoard, "Flip", { chessCtrl.flipBoard() })
                BottomBarButton(AppIcons.ResetBoard, "Reset", {
                    viewModel.navigator.clear()
                    chessCtrl.resetBoard()
                })
                BottomBarButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "Prev",
                    { viewModel.navigator.goPrevious() })
                BottomBarButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Next",
                    { viewModel.navigator.goNext() })
            }
        },
    )

    if (showBluetoothDialog) {
        BluetoothSessionDialog(
            sessionState = bluetoothState,
            pairedDevices = pairedDevices,
            onHost = { requestPermissionForHost() },
            onConnect = { device -> viewModel.connectBluetooth(device) },
            onRefreshDevices = { requestPermissionForRefresh() },
            onDisconnect = { viewModel.disconnectBluetooth() },
            onCancel = { viewModel.disconnectBluetooth() },
            onDismiss = { showBluetoothDialog = false },
        )
    }
}

@Composable
private fun BluetoothButton(state: BluetoothAnalysisSession.State, onClick: () -> Unit) {
    val tint = when (state) {
        is BluetoothAnalysisSession.State.Connected -> MaterialTheme.colorScheme.primary
        is BluetoothAnalysisSession.State.WaitingForPeer,
        is BluetoothAnalysisSession.State.Connecting -> MaterialTheme.colorScheme.secondary

        is BluetoothAnalysisSession.State.Idle -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Bluetooth,
            contentDescription = "Bluetooth analysis session",
            tint = tint,
        )
    }
}

@Composable
private fun BluetoothSessionDialog(
    sessionState: BluetoothAnalysisSession.State,
    pairedDevices: List<PairedDeviceUi>,
    onHost: () -> Unit,
    onConnect: (PairedDeviceUi) -> Unit,
    onRefreshDevices: () -> Unit,
    onDisconnect: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (sessionState) {
        is BluetoothAnalysisSession.State.Connected -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Connected to ${sessionState.peerName}") },
            text = { Text("Both of you can move pieces on the board freely.") },
            confirmButton = {
                Button(onClick = { onDisconnect(); onDismiss() }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Keep Connected") }
            },
        )

        is BluetoothAnalysisSession.State.WaitingForPeer,
        is BluetoothAnalysisSession.State.Connecting -> {
            val message = if (sessionState is BluetoothAnalysisSession.State.WaitingForPeer)
                "Waiting for peer to connect…"
            else "Connecting…"
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Bluetooth Analysis") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = { onCancel(); onDismiss() }) { Text("Cancel") }
                },
            )
        }

        is BluetoothAnalysisSession.State.Idle -> {
            var selectedTab by remember { mutableIntStateOf(0) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Bluetooth Analysis") },
                text = {
                    Column {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Host") },
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Join") },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        when (selectedTab) {
                            0 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Start a session and wait for the other device to join.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
                                    Text("Start Hosting")
                                }
                            }

                            1 -> Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "Paired devices",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = onRefreshDevices) { Text("Refresh") }
                                }
                                if (pairedDevices.isEmpty()) {
                                    Text(
                                        "No paired devices. Pair in system Bluetooth settings first.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                        items(pairedDevices, key = { it.address }) { device ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onConnect(device); onDismiss() }
                                                    .padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column {
                                                    Text(
                                                        device.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                    )
                                                    Text(
                                                        device.address,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                },
            )
        }
    }
}