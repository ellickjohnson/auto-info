package com.autoinfo.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.autoinfo.MainActivity
import com.autoinfo.R
import com.autoinfo.data.repository.TelemetryRepository
import com.autoinfo.domain.model.ConnectionState
import com.autoinfo.domain.model.TelemetryData
import com.autoinfo.domain.model.UploadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Foreground service for continuous OBD-II data collection
 */
@AndroidEntryPoint
class TelemetryService : Service() {
    
    @Inject
    lateinit var obdManager: ObdBluetoothManager
    
    @Inject
    lateinit var telemetryRepository: TelemetryRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectionJob: Job? = null
    
    private val binder = LocalBinder()
    
    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()
    
    private val _currentTelemetry = MutableStateFlow<TelemetryData?>(null)
    val currentTelemetry: StateFlow<TelemetryData?> = _currentTelemetry.asStateFlow()
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    private var vehicleId: String = ""
    private var pollingIntervalMs: Long = 1000L
    
    companion object {
        const val CHANNEL_ID = "telemetry_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.autoinfo.action.START"
        const val ACTION_STOP = "com.autoinfo.action.STOP"
        
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_POLLING_INTERVAL = "polling_interval"
        
        private const val TAG = "TelemetryService"
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): TelemetryService = this@TelemetryService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                vehicleId = intent.getStringExtra(EXTRA_VEHICLE_ID) ?: ""
                pollingIntervalMs = intent.getLongExtra(EXTRA_POLLING_INTERVAL, 1000L)
                startCollection()
            }
            ACTION_STOP -> {
                stopCollection()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCollection()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Collection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "OBD-II data collection in progress"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startCollection() {
        if (_isCollecting.value) return
        
        // Start foreground service
        val notification = createNotification("Collecting telemetry data...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        
        _isCollecting.value = true
        collectionJob = serviceScope.launch {
            collectTelemetryLoop()
        }
        
        Log.i(TAG, "Started telemetry collection for vehicle: $vehicleId")
    }
    
    private fun stopCollection() {
        _isCollecting.value = false
        collectionJob?.cancel()
        collectionJob = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Log.i(TAG, "Stopped telemetry collection")
    }
    
    private suspend fun collectTelemetryLoop() {
        while (_isCollecting.value && serviceScope.isActive) {
            try {
                // Check connection state
                when (val state = obdManager.connectionState.value) {
                    is ConnectionState.Connected -> {
                        // Read telemetry from OBD
                        val telemetry = obdManager.readTelemetry(vehicleId)
                        
                        if (telemetry != null) {
                            _currentTelemetry.value = telemetry
                            
                            // Save to local database
                            telemetryRepository.saveTelemetry(telemetry)
                            
                            // Try to upload to API
                            uploadTelemetry(telemetry)
                            
                            // Update notification
                            updateNotification("RPM: ${telemetry.rpm ?: "--"} | Speed: ${telemetry.speed ?: "--"} km/h")
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        Log.w(TAG, "OBD disconnected")
                        updateNotification("OBD disconnected - reconnecting...")
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "Connection error: ${state.message}")
                        updateNotification("Error: ${state.message}")
                    }
                    else -> {}
                }
                
                delay(pollingIntervalMs)
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in collection loop", e)
                delay(1000) // Wait before retry
            }
        }
    }
    
    private suspend fun uploadTelemetry(telemetry: TelemetryData) {
        _uploadState.value = UploadState.Uploading
        
        val result = telemetryRepository.uploadTelemetry(telemetry)
        
        _uploadState.value = if (result.isSuccess) {
            UploadState.Success
        } else {
            // Check if offline
            val pendingCount = telemetryRepository.getPendingCount()
            if (pendingCount > 0) {
                UploadState.Offline(pendingCount)
            } else {
                UploadState.Error(result.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto-Info")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }
}
