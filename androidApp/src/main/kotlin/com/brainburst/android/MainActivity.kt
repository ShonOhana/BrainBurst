package com.brainburst.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.brainburst.App
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.koin.core.context.loadKoinModules
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
//            .setTestDeviceIds(listOf("3A656B41B8712F1DE15746B335E66BC8"))
            .setTestDeviceIds(listOf("AC0320325CBF18E083F09182072FE7D4"))
            .build()
        MobileAds.setRequestConfiguration(testConfig)
        
        // Add activity module to existing Koin instance
        loadKoinModules(module {
            single<ComponentActivity> { this@MainActivity }
        })
        
        println("MainActivity: Activity module loaded into Koin")
        
        setContent {
            // Set status bar icons to dark
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = true // Makes status bar icons dark
                }
            }
            
            // Koin already initialized in Application, just use it
            App()
        }
    }
}


