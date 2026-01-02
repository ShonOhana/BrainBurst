import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("native.cocoapods")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    cocoapods {
        summary = "Shared module for BrainBurst"
        homepage = "https://github.com/brainburst"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        
        pod("FirebaseAuth") {
            version = "11.5.0"
        }
        pod("FirebaseFirestore") {
            version = "11.5.0"
        }
        // Temporarily disabled Google Sign-In
        // pod("GoogleSignIn") {
        //     version = "~> 7.1"
        // }
        
        framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Kotlin
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            
            // DataStore for persistence
            implementation(libs.datastore.preferences)
            
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            
            // Firebase (GitLive KMP)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.play.services.auth)
            implementation(libs.datastore.preferences.android)
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
            
            // AdMob for Android
            implementation("com.google.android.gms:play-services-ads:23.0.0")
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.brainburst.shared"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose {
    resources {
        publicResClass = true
    }
}

