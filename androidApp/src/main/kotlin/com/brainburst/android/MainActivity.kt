package com.brainburst.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brainburst.App
import com.brainburst.di.getAllModules
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AdMob
        MobileAds.initialize(this) {}

        val testConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("3A656B41B8712F1DE15746B335E66BC8")) // מזהה המכשיר שלך
            .build()
        MobileAds.setRequestConfiguration(testConfig)
        
        // Create a module that provides this activity instance
        val activityModule = module {
            single<ComponentActivity> { this@MainActivity }
        }
        
        setContent {
            // Combine platform modules with activity module
            App(koinModules = getAllModules() + activityModule)
        }
    }
}


