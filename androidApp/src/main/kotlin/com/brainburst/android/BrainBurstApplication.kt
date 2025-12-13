package com.brainburst.android

import android.app.Application

class BrainBurstApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is initialized automatically via google-services plugin
    }
}

