package com.example.chessrepertoiretrainer.feature.coursetransfer.presentation

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.bluetooth.BluetoothTransfer
import com.example.chessrepertoiretrainer.core.bluetooth.PairedDeviceUi
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.feature.repertoire.data.transfer.RepertoireExporter
import com.example.chessrepertoiretrainer.feature.repertoire.data.transfer.RepertoireImporter
import com.example.chessrepertoiretrainer.feature.repertoire.data.transfer.TransferEnvelope
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IncomingPreview(
    val envelope: TransferEnvelope,
    val fromName: String,
)

class CourseTransferViewModel(
    private val repertoireRepository: RepertoireRepository,
    private val bluetoothTransfer: BluetoothTransfer,
    private val exporter: RepertoireExporter,
    private val importer: RepertoireImporter,
) : ViewModel() {

    val repertoires: StateFlow<List<Repertoire>> =
        repertoireRepository.getAllRepertoires().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDeviceUi>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = _pairedDevices.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _isHosting = MutableStateFlow(false)
    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    private val _incomingPreview = MutableStateFlow<IncomingPreview?>(null)
    val incomingPreview: StateFlow<IncomingPreview?> = _incomingPreview.asStateFlow()

    private var sendJob: Job? = null
    private var receiveJob: Job? = null

    fun toggleSelect(id: Int) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun clearStatus() {
        _status.value = null
    }

    fun bluetoothReady(): Boolean =
        bluetoothTransfer.isBluetoothAvailable() && bluetoothTransfer.isBluetoothEnabled()

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val devices = bluetoothTransfer.pairedDevices().map {
            PairedDeviceUi(
                name = runCatching { it.name }.getOrNull() ?: "Unknown",
                address = it.address,
                device = it,
            )
        }
        _pairedDevices.value = devices
    }

    fun startSending() {
        if (_isHosting.value) return
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) {
            _status.value = "Pick at least one course"
            return
        }
        if (!bluetoothTransfer.hasConnectPermission()) {
            _status.value = "Bluetooth permission required"
            return
        }
        if (!bluetoothReady()) {
            _status.value = "Turn on Bluetooth"
            return
        }
        _isHosting.value = true
        _status.value = "Waiting for receiver…"
        sendJob = viewModelScope.launch {
            try {
                val payload = exporter.export(ids).toByteArray(Charsets.UTF_8)
                bluetoothTransfer.host(payload)
                _status.value = "Sent ${ids.size} course(s)"
            } catch (e: CancellationException) {
                _status.value = "Cancelled"
                throw e
            } catch (e: Throwable) {
                _status.value = "Send failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isHosting.value = false
            }
        }
    }

    fun cancelSending() {
        sendJob?.cancel()
    }

    fun startReceiving(deviceUi: PairedDeviceUi) {
        if (_isReceiving.value) return
        if (!bluetoothTransfer.hasConnectPermission()) {
            _status.value = "Bluetooth permission required"
            return
        }
        if (!bluetoothReady()) {
            _status.value = "Turn on Bluetooth"
            return
        }
        _isReceiving.value = true
        _status.value = "Connecting to ${deviceUi.name}…"
        receiveJob = viewModelScope.launch {
            try {
                val bytes = bluetoothTransfer.pull(deviceUi.device)
                val envelope = importer.parsePreview(String(bytes, Charsets.UTF_8))
                _incomingPreview.value = IncomingPreview(envelope, deviceUi.name)
                _status.value = null
            } catch (e: CancellationException) {
                _status.value = "Cancelled"
                throw e
            } catch (e: Throwable) {
                _status.value = "Receive failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isReceiving.value = false
            }
        }
    }

    fun cancelReceiving() {
        receiveJob?.cancel()
    }

    fun confirmImport() {
        val envelope = _incomingPreview.value?.envelope ?: return
        _incomingPreview.value = null
        viewModelScope.launch {
            val result = runCatching { importer.import(envelope) }
            result.onSuccess {
                _status.value = "Imported ${it.imported} course(s)"
            }.onFailure {
                _status.value = "Import failed: ${it.message ?: it.javaClass.simpleName}"
            }
        }
    }

    fun cancelImport() {
        _incomingPreview.value = null
        _status.value = "Import cancelled"
    }

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val bluetoothTransfer: BluetoothTransfer,
        private val exporter: RepertoireExporter,
        private val importer: RepertoireImporter,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CourseTransferViewModel(
                repertoireRepository,
                bluetoothTransfer,
                exporter,
                importer,
            ) as T
    }
}
