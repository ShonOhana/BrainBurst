package com.brainburst.di

import android.content.Context
import androidx.activity.ComponentActivity
import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.auth.GoogleSignInProvider
import com.brainburst.domain.notifications.NotificationManager
import com.brainburst.domain.share.ShareManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModule(): Module = module {
    // NotificationManager uses Application Context (available early from Application class)
    single { NotificationManager(get<Context>()) }
    
    // GoogleSignInProvider requires ComponentActivity (loaded later from MainActivity)
    single { GoogleSignInProvider(get<ComponentActivity>()) }
    
    // AdManager requires ComponentActivity
    single { AdManager(get<ComponentActivity>()) }
    
    // ShareManager requires Context (can use Activity when available)
    single { ShareManager(get<ComponentActivity>() as Context) }
    
    // Provide Android Context for DataStore (as Any to match common signature)
    single<Any> { get<Context>() }
}



