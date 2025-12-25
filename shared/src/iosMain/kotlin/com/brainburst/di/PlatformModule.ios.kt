package com.brainburst.di

import com.brainburst.domain.auth.GoogleSignInProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModule(): Module = module {
    single { GoogleSignInProvider() }
}


