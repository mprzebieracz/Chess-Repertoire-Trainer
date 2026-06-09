package com.example.chessrepertoiretrainer.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothAnalysisSession(private val context: Context) {

    companion object {
        val SESSION_UUID: UUID = UUID.fromString("7a4e2b1c-9f5d-4c8a-b3e6-2d1f8a9c5e7b")
        private const val SESSION_NAME = "ChessAnalysis"
    }

    sealed class State {
        object Idle : State()
        object WaitingForPeer : State()
        object Connecting : State()
        data class Connected(val peerName: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    // Emits UCI move strings received from the peer (e.g. "e2e4", "e7e8q")
    val incomingMoves = MutableSharedFlow<String>(extraBufferCapacity = 32)

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val adapter
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    suspend fun host() = withContext(Dispatchers.IO) {
        val a = adapter ?: return@withContext
        _state.value = State.WaitingForPeer
        var serverSocket: BluetoothServerSocket? = null
        var socket: BluetoothSocket? = null
        try {
            serverSocket = a.listenUsingRfcommWithServiceRecord(SESSION_NAME, SESSION_UUID)
            socket = serverSocket.accept()
            serverSocket.close()
            serverSocket = null
            val peerName = runCatching { socket.remoteDevice.name }.getOrNull() ?: "Peer"
            activeSocket = socket
            outputStream = socket.outputStream
            _state.value = State.Connected(peerName)
            readLoop(socket)
        }
        catch (_: IOException) {
        }
        catch (_: Exception) {
        }
        finally {
            _state.value = State.Idle
            runCatching { socket?.close() }
            runCatching { serverSocket?.close() }
            activeSocket = null
            outputStream = null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        val a = adapter ?: return@withContext
        _state.value = State.Connecting
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SESSION_UUID)
            if (a.isDiscovering) a.cancelDiscovery()
            socket.connect()
            val peerName = runCatching { device.name }.getOrNull() ?: "Peer"
            activeSocket = socket
            outputStream = socket.outputStream
            _state.value = State.Connected(peerName)
            readLoop(socket)
        }
        catch (_: IOException) {
        }
        catch (_: Exception) {
        }
        finally {
            _state.value = State.Idle
            runCatching { socket?.close() }
            activeSocket = null
            outputStream = null
        }
    }

    private suspend fun readLoop(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
        try {
            while (isActive) {
                val line = reader.readLine() ?: break
                if (line.startsWith("MOVE:")) {
                    incomingMoves.tryEmit(line.removePrefix("MOVE:"))
                }
            }
        }
        catch (_: IOException) {
        }
    }

    fun sendMove(uci: String) {
        runCatching {
            outputStream?.let { out ->
                out.write("MOVE:$uci\n".toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }

    fun disconnect() {
        runCatching { activeSocket?.close() }
    }
}