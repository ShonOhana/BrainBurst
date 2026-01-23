plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

android {
    namespace = "com.brainburst.android"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.brainburst.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.4"
    }
    
    buildFeatures {
        compose = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.play.services.auth)
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    
    // Firebase Cloud Messaging
    implementation(libs.firebase.messaging)
    
    // Firebase Crashlytics
    implementation(libs.firebase.crashlytics)
    
    // Firebase Analytics
    implementation(libs.firebase.analytics)
    
    // WorkManager for daily notifications
    implementation(libs.work.runtime)
}


