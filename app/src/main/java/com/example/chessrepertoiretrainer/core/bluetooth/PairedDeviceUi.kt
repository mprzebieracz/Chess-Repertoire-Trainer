package com.example.chessrepertoiretrainer.core.bluetooth

import android.bluetooth.BluetoothDevice

data class PairedDeviceUi(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
)
