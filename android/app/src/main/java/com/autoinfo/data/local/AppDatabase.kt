package com.autoinfo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autoinfo.data.local.entity.TelemetryEntity

@Database(
    entities = [TelemetryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
