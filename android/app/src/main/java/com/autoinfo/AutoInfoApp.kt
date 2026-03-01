package com.autoinfo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoInfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hilt handles DI automatically
    }
}
