package com.autoinfo.domain.model

/**
 * Represents a paired Bluetooth OBD-II adapter
 */
data class ObdDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean = false
)

/**
 * OBD-II PID (Parameter ID) definitions
 */
enum class ObdPid(val pid: String, val command: String, val description: String) {
    RPM("0C", "010C", "Engine RPM"),
    SPEED("0D", "010D", "Vehicle Speed"),
    COOLANT_TEMP("05", "0105", "Engine Coolant Temperature"),
    THROTTLE_POS("11", "0111", "Throttle Position"),
    FUEL_LEVEL("2F", "012F", "Fuel Tank Level"),
    ENGINE_LOAD("04", "0104", "Calculated Engine Load"),
    OIL_TEMP("5C", "015C", "Engine Oil Temperature"),
    BATTERY_VOLTAGE("42", "0142", "Control Module Voltage");
    
    companion object {
        fun fromPid(pid: String): ObdPid? = entries.find { it.pid == pid }
    }
}
