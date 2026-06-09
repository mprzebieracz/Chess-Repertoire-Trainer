package com.example.chessrepertoiretrainer.core.bluetooth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Requests BLUETOOTH_CONNECT + BLUETOOTH_SCAN on Android 12+; on older
 * versions the callback is invoked immediately with `granted = true`.
 * BLUETOOTH_SCAN is needed for cancelDiscovery() which is called before RFCOMM connect.
 */
@Composable
fun rememberBluetoothConnectPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions -> onResult(permissions.values.all { it }) },
    )
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                )
            )
        }
        else {
            onResult(true)
        }
    }
}