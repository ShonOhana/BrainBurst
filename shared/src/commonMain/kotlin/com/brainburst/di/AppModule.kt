package com.brainburst.di

import com.brainburst.data.repository.AuthRepositoryImpl
import com.brainburst.data.repository.GameStateRepositoryImpl
import com.brainburst.data.repository.PreferencesRepositoryImpl
import com.brainburst.data.repository.PuzzleRepositoryImpl
import com.brainburst.data.storage.createDataStore
import com.brainburst.domain.admin.AdminPuzzleUploader
import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.sudoku.Sudoku6x6Definition
import com.brainburst.domain.model.GameType
import com.brainburst.domain.notifications.NotificationManager
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.GameStateRepository
import com.brainburst.domain.repository.PreferencesRepository
import com.brainburst.domain.repository.PuzzleRepository
import com.brainburst.presentation.auth.AuthViewModel
import com.brainburst.presentation.home.HomeViewModel
import com.brainburst.presentation.leaderboard.LeaderboardViewModel
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.settings.SettingsViewModel
import com.brainburst.presentation.splash.SplashViewModel
import com.brainburst.presentation.sudoku.SudokuViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Coroutine Scope
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    
    // Firebase instances
    single { Firebase.auth }
    single { Firebase.firestore }
    
    // JSON serializer
    single { 
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }
    
    // DataStore for persistence (context provided by platform)
    single { createDataStore(getOrNull<Any>()) }
    
    // Game Engine
    single { 
        GameRegistry(
            games = listOf(
                Sudoku6x6Definition(get())
            )
        )
    }
    
    // Navigation
    single { Navigator() }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<PuzzleRepository> { PuzzleRepositoryImpl(get(), get()) }
    single<GameStateRepository> { GameStateRepositoryImpl(get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    
    // Admin utilities (for development/testing)
    single { AdminPuzzleUploader(get(), get()) }
    
    // ViewModels
    factory { SplashViewModel(get(), get(), get()) }
    factory { AuthViewModel(get(), get(), get(), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get(), get()) }  // Single scope to persist cache across navigation
    factory { SettingsViewModel(get(), get(), get(), get(), get()) }
    factory { SudokuViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { params -> LeaderboardViewModel(params.get(), get(), get(), get(), get(), get()) }
}

fun getAllModules() = listOf(appModule, getPlatformModule())

