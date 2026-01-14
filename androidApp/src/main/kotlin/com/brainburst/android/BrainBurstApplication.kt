package com.brainburst.android

import android.app.Application

class BrainBurstApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is initialized automatically via google-services plugin
        // Notifications are initialized in App composable when Koin is ready
    }
}

