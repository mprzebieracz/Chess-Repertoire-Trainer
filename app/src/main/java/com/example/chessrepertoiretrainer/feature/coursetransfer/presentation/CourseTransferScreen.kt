package com.example.chessrepertoiretrainer.feature.coursetransfer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.bluetooth.PairedDeviceUi
import com.example.chessrepertoiretrainer.core.bluetooth.rememberBluetoothConnectPermissionLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTransferScreen(
    viewModel: CourseTransferViewModel,
    onBackClick: () -> Unit,
) {
    val repertoires by viewModel.repertoires.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val isHosting by viewModel.isHosting.collectAsStateWithLifecycle()
    val isReceiving by viewModel.isReceiving.collectAsStateWithLifecycle()
    val incomingPreview by viewModel.incomingPreview.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val requestPermissionForSend = rememberBluetoothConnectPermissionLauncher { granted ->
        if (granted) viewModel.startSending() else viewModel.clearStatus()
    }
    val requestPermissionForRefresh = rememberBluetoothConnectPermissionLauncher { granted ->
        if (granted) viewModel.refreshPairedDevices()
    }

    LaunchedEffect(status) {
        val s = status
        if (!s.isNullOrBlank() && !isHosting && !isReceiving) {
            snackbarHostState.showSnackbar(s)
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && viewModel.bluetoothReady()) {
            requestPermissionForRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer courses") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Send") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Receive") },
                )
            }
            when (selectedTab) {
                0 -> SendTab(
                    repertoires = repertoires.map { it.id to it.name },
                    selectedIds = selectedIds,
                    isHosting = isHosting,
                    statusBanner = status.takeIf { isHosting },
                    onToggle = viewModel::toggleSelect,
                    onSend = { requestPermissionForSend() },
                    onCancel = viewModel::cancelSending,
                )
                1 -> ReceiveTab(
                    devices = pairedDevices,
                    isReceiving = isReceiving,
                    statusBanner = status.takeIf { isReceiving },
                    onRefresh = { requestPermissionForRefresh() },
                    onPick = viewModel::startReceiving,
                    onCancel = viewModel::cancelReceiving,
                )
            }
        }
    }

    val preview = incomingPreview
    if (preview != null) {
        val total = preview.envelope.repertoires.size
        val names = preview.envelope.repertoires.joinToString { it.name }
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Import $total course(s) from ${preview.fromName}?") },
            text = { Text(names.ifBlank { "(no courses in payload)" }) },
            confirmButton = {
                Button(onClick = viewModel::confirmImport, enabled = total > 0) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SendTab(
    repertoires: List<Pair<Int, String>>,
    selectedIds: Set<Int>,
    isHosting: Boolean,
    statusBanner: String?,
    onToggle: (Int) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (statusBanner != null) {
            StatusBanner(text = statusBanner)
            Spacer(Modifier.height(8.dp))
        }
        if (repertoires.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No courses to send.")
            }
        } else {
            Text(
                text = "Pick courses to send",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(repertoires, key = { it.first }) { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isHosting) { onToggle(id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = id in selectedIds,
                            onCheckedChange = { onToggle(id) },
                            enabled = !isHosting,
                        )
                        Spacer(Modifier.height(0.dp))
                        Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSend,
                enabled = !isHosting && selectedIds.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isHosting) "Waiting…" else "Send via Bluetooth")
            }
            if (isHosting) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ReceiveTab(
    devices: List<PairedDeviceUi>,
    isReceiving: Boolean,
    statusBanner: String?,
    onRefresh: () -> Unit,
    onPick: (PairedDeviceUi) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (statusBanner != null) {
            StatusBanner(text = statusBanner)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Paired devices",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefresh, enabled = !isReceiving) { Text("Refresh") }
        }
        Spacer(Modifier.height(4.dp))
        if (devices.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No paired devices. Pair in system Bluetooth settings.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices, key = { it.address }) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isReceiving) { onPick(device) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (isReceiving) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Column {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
