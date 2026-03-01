package com.autoinfo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoinfo.domain.model.TelemetryData

/**
 * Room entity for caching telemetry data locally
 */
@Entity(tableName = "telemetry_cache")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: String,
    val timestamp: Long,
    val rpm: Int?,
    val speed: Int?,
    val coolantTemp: Int?,
    val throttlePos: Int?,
    val fuelLevel: Int?,
    val engineLoad: Int?,
    val oilTemp: Int?,
    val batteryVoltage: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val isUploaded: Boolean = false
) {
    fun toTelemetryData(): TelemetryData {
        return TelemetryData(
            vehicleId = vehicleId,
            timestamp = timestamp,
            rpm = rpm,
            speed = speed,
            coolantTemp = coolantTemp,
            throttlePos = throttlePos,
            fuelLevel = fuelLevel,
            engineLoad = engineLoad,
            oilTemp = oilTemp,
            batteryVoltage = batteryVoltage,
            latitude = latitude,
            longitude = longitude
        )
    }
    
    companion object {
        fun fromTelemetryData(data: TelemetryData): TelemetryEntity {
            return TelemetryEntity(
                vehicleId = data.vehicleId,
                timestamp = data.timestamp,
                rpm = data.rpm,
                speed = data.speed,
                coolantTemp = data.coolantTemp,
                throttlePos = data.throttlePos,
                fuelLevel = data.fuelLevel,
                engineLoad = data.engineLoad,
                oilTemp = data.oilTemp,
                batteryVoltage = data.batteryVoltage,
                latitude = data.latitude,
                longitude = data.longitude
            )
        }
    }
}
