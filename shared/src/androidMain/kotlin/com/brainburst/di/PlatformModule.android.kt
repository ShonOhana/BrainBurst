package com.brainburst.di

import android.content.Context
import androidx.activity.ComponentActivity
import com.brainburst.domain.auth.GoogleSignInProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModule(): Module = module {
    // GoogleSignInProvider requires ComponentActivity
    // This will be set from MainActivity
    single { GoogleSignInProvider(get<ComponentActivity>()) }
    
    // Provide Android Context for DataStore (as Any to match common signature)
    single<Any> { get<ComponentActivity>() as Context }
}



