package com.brainburst.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brainburst.App
import com.brainburst.di.getAllModules
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase Analytics
        analytics = Firebase.analytics
        
        // Initialize AdMob
        MobileAds.initialize(this) {}

        val testConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("3A656B41B8712F1DE15746B335E66BC8"))
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


