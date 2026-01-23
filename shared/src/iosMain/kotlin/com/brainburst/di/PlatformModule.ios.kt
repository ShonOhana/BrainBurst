package com.brainburst.di

import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.auth.GoogleSignInProvider
import com.brainburst.domain.notifications.NotificationManager
import com.brainburst.domain.share.ShareManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModule(): Module = module {
    single { GoogleSignInProvider() }
    
    // AdManager for iOS (no UIViewController needed for MVP)
    single { AdManager() }
    
    // NotificationManager for iOS
    single { NotificationManager() }
    
    // ShareManager for iOS - temporarily disabled
    single { ShareManager() }
}



