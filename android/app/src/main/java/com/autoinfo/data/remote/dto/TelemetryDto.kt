package com.autoinfo.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for telemetry upload to the API
 */
data class TelemetryUploadDto(
    @SerializedName("vehicle_id")
    val vehicleId: String,
    @SerializedName("rpm")
    val rpm: Int?,
    @SerializedName("speed")
    val speed: Int?,
    @SerializedName("coolant_temp")
    val coolantTemp: Int?,
    @SerializedName("throttle_pos")
    val throttlePos: Int?,
    @SerializedName("fuel_level")
    val fuelLevel: Int?,
    @SerializedName("oil_temp")
    val oilTemp: Int?,
    @SerializedName("battery_voltage")
    val batteryVoltage: Double?,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?
)

/**
 * Response from the API after telemetry upload
 */
data class TelemetryResponseDto(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("vehicle_id")
    val vehicleId: String,
    @SerializedName("time")
    val time: String?,
    @SerializedName("rpm")
    val rpm: Int?,
    @SerializedName("speed")
    val speed: Int?,
    @SerializedName("coolant_temp")
    val coolantTemp: Int?,
    @SerializedName("throttle_pos")
    val throttlePos: Int?,
    @SerializedName("fuel_level")
    val fuelLevel: Int?
)
