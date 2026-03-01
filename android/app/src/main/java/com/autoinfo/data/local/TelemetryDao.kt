package com.autoinfo.data.local

import androidx.room.*
import com.autoinfo.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(entity: TelemetryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TelemetryEntity>)
    
    @Query("SELECT * FROM telemetry_cache WHERE vehicleId = :vehicleId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getTelemetryForVehicle(vehicleId: String, limit: Int = 100): List<TelemetryEntity>
    
    @Query("SELECT * FROM telemetry_cache WHERE isUploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUpload(): List<TelemetryEntity>
    
    @Query("SELECT COUNT(*) FROM telemetry_cache WHERE isUploaded = 0")
    suspend fun getPendingCount(): Int
    
    @Query("SELECT COUNT(*) FROM telemetry_cache WHERE isUploaded = 0")
    fun getPendingCountFlow(): Flow<Int>
    
    @Update
    suspend fun updateTelemetry(entity: TelemetryEntity)
    
    @Query("UPDATE telemetry_cache SET isUploaded = 1 WHERE id IN (:ids)")
    suspend fun markAsUploaded(ids: List<Long>)
    
    @Query("DELETE FROM telemetry_cache WHERE isUploaded = 1 AND timestamp < :olderThan")
    suspend fun deleteOldUploaded(olderThan: Long)
    
    @Query("DELETE FROM telemetry_cache")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM telemetry_cache WHERE vehicleId = :vehicleId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForVehicle(vehicleId: String): TelemetryEntity?
}
