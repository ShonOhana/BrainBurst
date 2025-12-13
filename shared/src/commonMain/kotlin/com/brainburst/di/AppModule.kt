package com.brainburst.di

import com.brainburst.data.repository.AuthRepositoryImpl
import com.brainburst.data.repository.PuzzleRepositoryImpl
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.PuzzleRepository
import com.brainburst.presentation.auth.AuthViewModel
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.splash.SplashViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Coroutine Scope
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    
    // Firebase instances
    single { Firebase.auth }
    single { Firebase.firestore }
    
    // Navigation
    single { Navigator() }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<PuzzleRepository> { PuzzleRepositoryImpl(get()) }
    
    // ViewModels
    factory { SplashViewModel(get(), get(), get()) }
    factory { AuthViewModel(get(), get(), get()) }
}

