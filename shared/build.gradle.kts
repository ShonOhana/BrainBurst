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
        pod("FirebaseAnalytics") {
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
            
            // WorkManager for notifications
            implementation(libs.work.runtime)
            
            // Firebase Cloud Messaging
            implementation(libs.firebase.messaging)
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

// Fix deployment target for synthetic iOS Podfile
tasks.register("fixSyntheticPodfile") {
    doLast {
        val syntheticPodfile = file("build/cocoapods/synthetic/ios/Podfile")
        if (syntheticPodfile.exists()) {
            var content = syntheticPodfile.readText()
            // Replace the deployment target check from 11.0 to 15.0
            content = content.replace(
                "if deployment_target_major < 11 || (deployment_target_major == 11 && deployment_target_minor < 0) then\n            version = \"#{11}.#{0}\"",
                "if deployment_target_major < 15 || (deployment_target_major == 15 && deployment_target_minor < 0) then\n            version = \"#{15}.#{0}\""
            )
            syntheticPodfile.writeText(content)
            println("Fixed deployment target to 15.0 in synthetic Podfile")
        }
    }
}

// Run the fix after pod generation
tasks.named("podGenIos").configure {
    finalizedBy("fixSyntheticPodfile")
}

