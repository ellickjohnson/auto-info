package com.autoinfo.data.repository

import android.util.Log
import com.autoinfo.data.local.TelemetryDao
import com.autoinfo.data.local.entity.TelemetryEntity
import com.autoinfo.data.remote.AutoInfoApi
import com.autoinfo.data.remote.dto.TelemetryUploadDto
import com.autoinfo.domain.model.TelemetryData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for telemetry data - handles local caching and remote sync
 */
@Singleton
class TelemetryRepository @Inject constructor(
    private val api: AutoInfoApi,
    private val dao: TelemetryDao
) {
    companion object {
        private const val TAG = "TelemetryRepository"
    }
    
    /**
     * Save telemetry to local database
     */
    suspend fun saveTelemetry(data: TelemetryData) {
        val entity = TelemetryEntity.fromTelemetryData(data)
        dao.insertTelemetry(entity)
        Log.d(TAG, "Saved telemetry for vehicle: ${data.vehicleId}")
    }
    
    /**
     * Upload telemetry to API, fallback to local cache if offline
     */
    suspend fun uploadTelemetry(data: TelemetryData): Result<Unit> {
        val dto = TelemetryUploadDto(
            vehicleId = data.vehicleId,
            rpm = data.rpm,
            speed = data.speed,
            coolantTemp = data.coolantTemp,
            throttlePos = data.throttlePos,
            fuelLevel = data.fuelLevel,
            oilTemp = data.oilTemp,
            batteryVoltage = data.batteryVoltage,
            latitude = data.latitude,
            longitude = data.longitude
        )
        
        return try {
            val response = api.uploadTelemetry(dto)
            if (response.isSuccessful) {
                // Mark local records as uploaded
                Log.d(TAG, "Uploaded telemetry for vehicle: ${data.vehicleId}")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Upload failed: ${response.code()}")
                Result.failure(Exception("Server returned ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload failed (offline?): ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Sync pending local data to server
     */
    suspend fun syncPendingUploads(): Int {
        val pending = dao.getPendingUpload()
        var synced = 0
        
        for (entity in pending) {
            val data = entity.toTelemetryData()
            val result = uploadTelemetry(data)
            
            if (result.isSuccess) {
                dao.markAsUploaded(listOf(entity.id))
                synced++
            } else {
                // Stop on first failure - likely offline
                break
            }
        }
        
        Log.i(TAG, "Synced $synced pending records")
        return synced
    }
    
    /**
     * Get count of pending uploads
     */
    suspend fun getPendingCount(): Int = dao.getPendingCount()
    
    /**
     * Get pending count as Flow for UI observation
     */
    fun getPendingCountFlow(): Flow<Int> = dao.getPendingCountFlow()
    
    /**
     * Get historical telemetry for a vehicle
     */
    suspend fun getTelemetryHistory(vehicleId: String, limit: Int = 100): List<TelemetryData> {
        return dao.getTelemetryForVehicle(vehicleId, limit).map { it.toTelemetryData() }
    }
    
    /**
     * Get latest telemetry for a vehicle
     */
    suspend fun getLatestTelemetry(vehicleId: String): TelemetryData? {
        return dao.getLatestForVehicle(vehicleId)?.toTelemetryData()
    }
    
    /**
     * Clean up old uploaded data
     */
    suspend fun cleanupOldData(olderThanTimestamp: Long) {
        dao.deleteOldUploaded(olderThanTimestamp)
    }
}
