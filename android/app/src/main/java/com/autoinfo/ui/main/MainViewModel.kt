package com.autoinfo.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoinfo.bluetooth.ObdBluetoothManager
import com.autoinfo.bluetooth.TelemetryService
import com.autoinfo.data.repository.TelemetryRepository
import com.autoinfo.domain.model.ConnectionState
import com.autoinfo.domain.model.ObdDevice
import com.autoinfo.domain.model.TelemetryData
import com.autoinfo.domain.model.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val obdManager: ObdBluetoothManager,
    private val telemetryRepository: TelemetryRepository
) : AndroidViewModel(application) {
    
    private val context = application
    
    // Settings keys
    private val vehicleIdKey = stringPreferencesKey("vehicle_id")
    private val apiUrlKey = stringPreferencesKey("api_url")
    private val pollingIntervalKey = longPreferencesKey("polling_interval")
    
    // State
    private val _vehicleId = MutableStateFlow("")
    val vehicleId: StateFlow<String> = _vehicleId.asStateFlow()
    
    private val _apiUrl = MutableStateFlow("https://api.auto-info.ellickjohnson.net")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()
    
    private val _pollingInterval = MutableStateFlow(1000L)
    val pollingInterval: StateFlow<Long> = _pollingInterval.asStateFlow()
    
    private val _pairedDevices = MutableStateFlow<List<ObdDevice>>(emptyList())
    val pairedDevices: StateFlow<List<ObdDevice>> = _pairedDevices.asStateFlow()
    
    private val _selectedDevice = MutableStateFlow<ObdDevice?>(null)
    val selectedDevice: StateFlow<ObdDevice?> = _selectedDevice.asStateFlow()
    
    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()
    
    private val _currentTelemetry = MutableStateFlow<TelemetryData?>(null)
    val currentTelemetry: StateFlow<TelemetryData?> = _currentTelemetry.asStateFlow()
    
    // Connection state from OBD manager
    val connectionState: StateFlow<ConnectionState> = obdManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Lazily, ConnectionState.Disconnected)
    
    // Pending uploads from repository
    val pendingUploadCount: StateFlow<Int> = telemetryRepository.getPendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    // Service binding
    private var telemetryService: TelemetryService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TelemetryService.LocalBinder
            telemetryService = binder.getService()
            isBound = true
            
            // Observe service state
            viewModelScope.launch {
                telemetryService?.currentTelemetry?.collect { telemetry ->
                    _currentTelemetry.value = telemetry
                }
            }
            viewModelScope.launch {
                telemetryService?.isCollecting?.collect { collecting ->
                    _isCollecting.value = collecting
                }
            }
            viewModelScope.launch {
                telemetryService?.uploadState?.collect { state ->
                    _uploadState.value = state
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            telemetryService = null
            isBound = false
        }
    }
    
    init {
        loadSettings()
        loadPairedDevices()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _vehicleId.value = prefs[vehicleIdKey] ?: ""
            _apiUrl.value = prefs[apiUrlKey] ?: "https://api.auto-info.ellickjohnson.net"
            _pollingInterval.value = prefs[pollingIntervalKey] ?: 1000L
        }
    }
    
    private fun loadPairedDevices() {
        _pairedDevices.value = obdManager.getPairedDevices()
    }
    
    fun setVehicleId(id: String) {
        _vehicleId.value = id
        viewModelScope.launch {
            context.dataStore.edit { it[vehicleIdKey] = id }
        }
    }
    
    fun setApiUrl(url: String) {
        _apiUrl.value = url
        viewModelScope.launch {
            context.dataStore.edit { it[apiUrlKey] = url }
        }
    }
    
    fun setPollingInterval(intervalMs: Long) {
        _pollingInterval.value = intervalMs
        viewModelScope.launch {
            context.dataStore.edit { it[pollingIntervalKey] = intervalMs }
        }
    }
    
    fun selectDevice(device: ObdDevice) {
        _selectedDevice.value = device
    }
    
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            obdManager.connect(deviceAddress)
        }
    }
    
    fun disconnect() {
        obdManager.disconnect()
        _selectedDevice.value = null
    }
    
    fun startCollection() {
        val device = _selectedDevice.value ?: return
        val vehicle = _vehicleId.value.ifEmpty { return }
        
        // Ensure connected
        if (connectionState.value !is ConnectionState.Connected) {
            connectToDevice(device.address)
        }
        
        // Start service
        val intent = Intent(context, TelemetryService::class.java).apply {
            action = TelemetryService.ACTION_START
            putExtra(TelemetryService.EXTRA_VEHICLE_ID, vehicle)
            putExtra(TelemetryService.EXTRA_POLLING_INTERVAL, _pollingInterval.value)
        }
        context.startService(intent)
        
        // Bind to service
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun stopCollection() {
        val intent = Intent(context, TelemetryService::class.java).apply {
            action = TelemetryService.ACTION_STOP
        }
        context.startService(intent)
        
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        
        _isCollecting.value = false
    }
    
    fun syncPendingData() {
        viewModelScope.launch {
            telemetryRepository.syncPendingUploads()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
        }
    }
}
