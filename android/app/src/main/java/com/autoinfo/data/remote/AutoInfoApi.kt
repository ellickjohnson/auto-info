package com.autoinfo.data.remote

import com.autoinfo.data.remote.dto.TelemetryResponseDto
import com.autoinfo.data.remote.dto.TelemetryUploadDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for the Auto-Info backend
 */
interface AutoInfoApi {
    
    /**
     * Upload telemetry data
     */
    @POST("api/telemetry")
    suspend fun uploadTelemetry(@Body data: TelemetryUploadDto): Response<TelemetryResponseDto>
    
    /**
     * Get historical telemetry for a vehicle
     */
    @GET("api/telemetry/{vehicleId}")
    suspend fun getTelemetry(
        @Path("vehicleId") vehicleId: String,
        @Query("limit") limit: Int = 100
    ): Response<List<TelemetryResponseDto>>
    
    /**
     * Get latest telemetry for a vehicle
     */
    @GET("api/telemetry/{vehicleId}/latest")
    suspend fun getLatestTelemetry(@Path("vehicleId") vehicleId: String): Response<TelemetryResponseDto>
    
    /**
     * Health check
     */
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
