package com.autoinfo.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.autoinfo.domain.model.ConnectionState
import com.autoinfo.domain.model.ObdDevice
import com.autoinfo.domain.model.ObdPid
import com.autoinfo.domain.model.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles communication with ELM327 OBD-II Bluetooth adapters
 */
@Singleton
class ObdBluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var socket: BluetoothSocket? = null
    private var isInitialized = false
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    companion object {
        private const val TAG = "ObdBluetoothManager"
        // Standard SPP UUID for most ELM327 adapters
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // Common ELM327 device name patterns
        private val OBD_DEVICE_PATTERNS = listOf(
            "OBDII", "OBD2", "OBD", "ELM327", "V-LINK", "AUTO", "CAR"
        )
    }
    
    /**
     * Get list of paired Bluetooth devices that are likely OBD adapters
     */
    fun getPairedDevices(): List<ObdDevice> {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not available")
            return emptyList()
        }
        
        return bluetoothAdapter.bondedDevices
            .filter { device -> 
                OBD_DEVICE_PATTERNS.any { pattern -> 
                    device.name?.uppercase()?.contains(pattern) == true 
                }
            }
            .map { device ->
                ObdDevice(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    isPaired = true
                )
            }
    }
    
    /**
     * Connect to an OBD-II adapter
     */
    suspend fun connect(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
            return@withContext false
        }
        
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            _connectionState.value = ConnectionState.Error("Device not found")
            return@withContext false
        }
        
        _connectionState.value = ConnectionState.Connecting(device.name ?: "Unknown")
        
        return@withContext try {
            // Create RFCOMM socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            
            // Initialize ELM327 adapter
            val initSuccess = initializeElm327()
            
            if (initSuccess) {
                isInitialized = true
                _connectionState.value = ConnectionState.Connected(device.name ?: "Unknown")
                Log.i(TAG, "Connected to ${device.name}")
                true
            } else {
                disconnect()
                _connectionState.value = ConnectionState.Error("Failed to initialize ELM327")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            _connectionState.value = ConnectionState.Error("Bluetooth permission denied")
            false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect", e)
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            false
        }
    }
    
    /**
     * Disconnect from the OBD-II adapter
     */
    fun disconnect() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket", e)
        }
        socket = null
        isInitialized = false
        _connectionState.value = ConnectionState.Disconnected
    }
    
    /**
     * Initialize ELM327 adapter with AT commands
     */
    private suspend fun initializeElm327(): Boolean = withContext(Dispatchers.IO) {
        val commands = listOf(
            "ATZ" to "ELM327 reset",      // Reset
            "ATE0" to "Echo off",          // Echo off
            "ATL0" to "Linefeeds off",     // Linefeeds off
            "ATS0" to "Spaces off",        // Spaces off
            "ATH0" to "Headers off",       // Headers off
            "ATSP0" to "Auto protocol",    // Auto protocol
            "0100" to "Init PIDs"          // Initialize PIDs
        )
        
        for ((command, description) in commands) {
            val response = sendCommand(command)
            if (response == null) {
                Log.w(TAG, "No response for $description")
                return@withContext false
            }
            // Small delay between commands
            Thread.sleep(100)
        }
        
        Log.i(TAG, "ELM327 initialized successfully")
        true
    }
    
    /**
     * Read current telemetry data from the OBD-II adapter
     */
    suspend fun readTelemetry(vehicleId: String): TelemetryData? = withContext(Dispatchers.IO) {
        if (socket == null || !isInitialized) {
            Log.w(TAG, "Not connected to OBD adapter")
            return@withContext null
        }
        
        try {
            val rpm = readPid(ObdPid.RPM)
            val speed = readPid(ObdPid.SPEED)
            val coolantTemp = readPid(ObdPid.COOLANT_TEMP)
            val throttlePos = readPid(ObdPid.THROTTLE_POS)
            val fuelLevel = readPid(ObdPid.FUEL_LEVEL)
            val engineLoad = readPid(ObdPid.ENGINE_LOAD)
            
            TelemetryData(
                vehicleId = vehicleId,
                rpm = rpm,
                speed = speed,
                coolantTemp = coolantTemp,
                throttlePos = throttlePos,
                fuelLevel = fuelLevel,
                engineLoad = engineLoad
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading telemetry", e)
            null
        }
    }
    
    /**
     * Read a specific OBD-II PID
     */
    private fun readPid(pid: ObdPid): Int? {
        val response = sendCommand(pid.command) ?: return null
        
        return try {
            parsePidResponse(pid, response)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse ${pid.description}: $response")
            null
        }
    }
    
    /**
     * Send a command to the ELM327 and get the response
     */
    private fun sendCommand(command: String): String? {
        val sock = socket ?: return null
        
        return try {
            val outputStream = sock.outputStream
            val inputStream = sock.inputStream
            
            // Send command with carriage return
            outputStream.write("$command\r".toByteArray())
            outputStream.flush()
            
            // Read response with timeout
            val buffer = ByteArray(256)
            val startTime = System.currentTimeMillis()
            var totalBytes = 0
            var foundPrompt = false
            
            while (System.currentTimeMillis() - startTime < 2000 && !foundPrompt) {
                if (inputStream.available() > 0) {
                    val bytesRead = inputStream.read(buffer, totalBytes, buffer.size - totalBytes)
                    if (bytesRead > 0) {
                        totalBytes += bytesRead
                        // Check for prompt character (>)
                        if (buffer.contains('>'.code.toByte())) {
                            foundPrompt = true
                        }
                    }
                }
                Thread.sleep(10)
            }
            
            if (totalBytes == 0) return null
            
            val response = String(buffer, 0, totalBytes).trim()
            Log.d(TAG, "Command: $command -> Response: $response")
            
            // Clean response (remove command echo, prompt, etc)
            cleanResponse(response)
        } catch (e: IOException) {
            Log.e(TAG, "Error sending command: $command", e)
            null
        }
    }
    
    /**
     * Clean the response from ELM327
     */
    private fun cleanResponse(response: String): String {
        return response
            .replace(">", "")
            .replace("\r", "")
            .replace("\n", "")
            .trim()
    }
    
    /**
     * Parse PID response based on the PID type
     */
    private fun parsePidResponse(pid: ObdPid, response: String): Int? {
        // Response format: 41 XX YY YY (where XX is PID, YY YY is data)
        // Example: 410C 1A F8 -> RPM = (0x1AF8) / 4 = 1724
        
        // Find the actual data bytes (skip "41" and PID)
        val hexData = response
            .replace(" ", "")
            .replace("41${pid.pid}", "")
            .uppercase()
        
        if (hexData.length < 4) return null
        
        return when (pid) {
            ObdPid.RPM -> {
                // 2 bytes, divide by 4
                val a = hexData.substring(0, 2).toInt(16)
                val b = hexData.substring(2, 4).toInt(16)
                ((a * 256) + b) / 4
            }
            ObdPid.SPEED -> {
                // 1 byte, direct km/h
                hexData.substring(0, 2).toInt(16)
            }
            ObdPid.COOLANT_TEMP -> {
                // 1 byte, subtract 40 for Celsius
                hexData.substring(0, 2).toInt(16) - 40
            }
            ObdPid.THROTTLE_POS -> {
                // 1 byte, (value * 100) / 255 for percentage
                (hexData.substring(0, 2).toInt(16) * 100) / 255
            }
            ObdPid.FUEL_LEVEL -> {
                // 1 byte, (value * 100) / 255 for percentage
                (hexData.substring(0, 2).toInt(16) * 100) / 255
            }
            ObdPid.ENGINE_LOAD -> {
                // 1 byte, (value * 100) / 255 for percentage
                (hexData.substring(0, 2).toInt(16) * 100) / 255
            }
            else -> null
        }
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}
