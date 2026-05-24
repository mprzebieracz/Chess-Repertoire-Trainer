package com.example.chessrepertoiretrainer.core.bluetooth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Returns a launcher that requests BLUETOOTH_CONNECT on Android 12+; on older
 * versions the callback is invoked immediately with `granted = true`.
 */
@Composable
fun rememberBluetoothConnectPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            onResult(true)
        }
    }
}
