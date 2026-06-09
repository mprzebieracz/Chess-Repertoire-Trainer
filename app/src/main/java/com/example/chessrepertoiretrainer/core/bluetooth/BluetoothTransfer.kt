package com.example.chessrepertoiretrainer.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

class BluetoothTransfer(private val context: Context) {

    companion object {
        // App-specific RFCOMM service UUID. Both phones must use the same one.
        val SERVICE_UUID: UUID = UUID.fromString("8b58e1f0-7d76-4c2a-9d0e-7a3f3a8f5d1b")
        private const val SERVICE_NAME = "ChessRepertoireCourseTransfer"
        private const val MAX_PAYLOAD_SIZE = 32 * 1024 * 1024 // 32 MB ceiling
    }

    private val adapter: BluetoothAdapter?
        get() {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return manager?.adapter
        }

    fun isBluetoothAvailable(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDevice> {
        if (!hasConnectPermission()) return emptyList()
        val a = adapter ?: return emptyList()
        if (!a.isEnabled) return emptyList()
        return runCatching { a.bondedDevices.toList() }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    suspend fun host(payload: ByteArray): Unit = withContext(Dispatchers.IO) {
        require(hasConnectPermission()) { "BLUETOOTH_CONNECT permission required" }
        val bt = adapter ?: error("Bluetooth not available")
        require(bt.isEnabled) { "Bluetooth is off" }

        var server: BluetoothServerSocket? = null
        var socket: BluetoothSocket? = null
        try {
            server = bt.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            socket = server.accept()
            server.close()
            server = null
            writePayload(socket, payload)
        }
        finally {
            runCatching { socket?.close() }
            runCatching { server?.close() }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun pull(device: BluetoothDevice): ByteArray = withContext(Dispatchers.IO) {
        require(hasConnectPermission()) { "BLUETOOTH_CONNECT permission required" }
        val bt = adapter ?: error("Bluetooth not available")
        require(bt.isEnabled) { "Bluetooth is off" }

        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            if (bt.isDiscovering) bt.cancelDiscovery()
            socket.connect()
            readPayload(socket)
        }
        finally {
            runCatching { socket?.close() }
        }
    }

    private fun writePayload(socket: BluetoothSocket, payload: ByteArray) {
        val out = DataOutputStream(socket.outputStream)
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
        // Wait for receiver's ACK before closing so the socket isn't torn down
        // while the other side is still reading the buffered data.
        socket.inputStream.read()
    }

    private fun readPayload(socket: BluetoothSocket): ByteArray {
        val input = DataInputStream(socket.inputStream)
        val size = input.readInt()
        if (size < 0 || size > MAX_PAYLOAD_SIZE) {
            throw IOException("Invalid payload size: $size")
        }
        val buffer = ByteArray(size)
        input.readFully(buffer)
        // Send ACK so the sender knows it's safe to close.
        socket.outputStream.write(0)
        socket.outputStream.flush()
        return buffer
    }

    fun newAnalysisSession() = BluetoothAnalysisSession(context)
}