package com.autoinfo.domain.model

/**
 * Represents a single telemetry reading from the OBD-II adapter
 */
data class TelemetryData(
    val vehicleId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rpm: Int? = null,
    val speed: Int? = null,           // km/h
    val coolantTemp: Int? = null,      // Celsius
    val throttlePos: Int? = null,      // Percentage 0-100
    val fuelLevel: Int? = null,        // Percentage 0-100
    val engineLoad: Int? = null,       // Percentage 0-100
    val oilTemp: Int? = null,          // Celsius
    val batteryVoltage: Double? = null, // Volts
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Connection state of the OBD-II adapter
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Upload state for telemetry data
 */
sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data object Success : UploadState()
    data class Error(val message: String) : UploadState()
    data class Offline(val pendingCount: Int) : UploadState()
}
