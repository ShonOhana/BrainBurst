package com.brainburst.android

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import com.brainburst.di.appModule
import com.brainburst.di.getPlatformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class BrainBurstApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin with all app modules early so Workers can access dependencies
        startKoin {
            androidContext(this@BrainBurstApplication)
            modules(
                appModule,
                getPlatformModule(),
                // Provide Application Context for NotificationManager and DataStore
                module {
                    single<Context> { this@BrainBurstApplication as Context }
                }
            )
        }
        
        println("BrainBurstApplication: Koin initialized with Application Context")
        
        // Firebase is initialized automatically via google-services plugin
    }
}

