# Google Sign-In Setup Guide

This guide will help you configure Google Sign-In for both Android and iOS.

## Prerequisites

You need to have a Firebase project set up with:
- `google-services.json` for Android
- `GoogleService-Info.plist` for iOS

## Android Setup

### Step 1: Add SHA-1 Fingerprint to Firebase

**This is required for Google Sign-In to work!**

#### Get Your SHA-1 Fingerprint

**Option A: Using Gradle (Easiest)**

In your project terminal, run:
```bash
./gradlew signingReport
```

Look for the **debug** variant and copy the **SHA-1** value. It will look like:
```
SHA1: A1:B2:C3:D4:E5:F6:G7:H8:I9:J0:K1:L2:M3:N4:O5:P6:Q7:R8:S9:T0
```

**Option B: Using keytool**

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint from the output.

#### Add SHA-1 to Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (brainburst-bb78e)
3. Click the **⚙️ Settings** icon → **Project Settings**
4. Scroll down to **Your apps** section
5. Find your Android app (`com.brainburst.android`)
6. Click **Add fingerprint**
7. Paste your SHA-1 fingerprint
8. Click **Save**

**Important:** You'll need to add SHA-1 for:
- **Debug builds** (from ~/.android/debug.keystore) - for development
- **Release builds** (from your release keystore) - when you publish the app

### Step 2: Enable Google Sign-In in Firebase Console

1. Navigate to **Authentication** → **Sign-in method**
2. Click on **Google** in the sign-in providers list
3. Toggle **Enable**
4. Select your support email from the dropdown
5. Click **Save**

### Step 3: Get Your Web Client ID

After enabling Google Sign-In in Step 2, you'll get a Web Client ID:

1. In the Google Sign-In configuration screen (where you just enabled it)
2. Scroll down to **Web SDK configuration** section
3. Copy the **Web client ID** that appears (it auto-generates after you save)

**OR** find it in Project Settings:

1. Go to **Project Settings** (⚙️ icon)
2. Scroll down to **Your apps** section
3. Under **OAuth 2.0 Client IDs**, find **Web client (auto created by Google Service)**
4. Copy the **Client ID**

It will look like: `720231725608-abc123xyz789.apps.googleusercontent.com`

### Step 4: Update the Web Client ID in Code

Open this file:
```
shared/src/androidMain/kotlin/com/brainburst/domain/auth/GoogleSignInProvider.android.kt
```

Replace this line:
```kotlin
private val webClientId = "720231725608-YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

With your actual Web Client ID:
```kotlin
private val webClientId = "720231725608-abc123xyz456.apps.googleusercontent.com"
```

### Step 5: Download Updated google-services.json

After adding SHA-1 and enabling Google Sign-In, download a fresh `google-services.json`:

1. In Firebase Console → **Project Settings**
2. Scroll to **Your apps** → Select your Android app
3. Click **Download google-services.json**
4. Replace the existing file at `androidApp/google-services.json`

The new file should include `oauth_client` entries with your Web Client ID.

### Step 6: Sync and Build

After updating the files:
```bash
# Sync Gradle
./gradlew clean

# Build the app
./gradlew :androidApp:assembleDebug
```

### Step 7: Test on Android

1. Run the Android app
2. On the Auth screen, tap **Continue with Google**
3. Select your Google account
4. You should be signed in and navigated to the Home screen
5. Tap the logout icon (↗️) in the top bar to test logout

## iOS Setup

### Step 1: Enable Google Sign-In in Firebase Console

Follow the same steps as Android Step 1 above.

### Step 2: Add Google Sign-In Pod

Add the Google Sign-In SDK to your `iosApp/Podfile`:

```ruby
target 'iosApp' do
  use_frameworks!
  platform :ios, '14.0'
  
  # ... existing pods ...
  
  # Add this line:
  pod 'GoogleSignIn', '~> 7.0'
end
```

Then run:
```bash
cd iosApp
pod install
```

### Step 3: Configure URL Scheme

1. Open `iosApp/iosApp.xcworkspace` in Xcode (NOT .xcodeproj)
2. Select your project in the navigator
3. Select the **iosApp** target
4. Go to the **Info** tab
5. Expand **URL Types**
6. Add a new URL Type:
   - **Identifier**: `com.googleusercontent.apps.REVERSED_CLIENT_ID`
   - **URL Schemes**: Your reversed client ID from `GoogleService-Info.plist`
   
To find your reversed client ID:
```bash
grep REVERSED_CLIENT_ID iosApp/iosApp/GoogleService-Info.plist
```

### Step 4: Implement iOS Google Sign-In

Update `shared/src/iosMain/kotlin/com/brainburst/domain/auth/GoogleSignInProvider.ios.kt`:

```kotlin
package com.brainburst.domain.auth

import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class GoogleSignInProvider {
    
    actual suspend fun signIn(): String = suspendCancellableCoroutine { continuation ->
        val presentingViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        
        if (presentingViewController == null) {
            continuation.resumeWithException(Exception("No presenting view controller"))
            return@suspendCancellableCoroutine
        }
        
        GIDSignIn.sharedInstance.signInWithPresentingViewController(
            presentingViewController
        ) { result, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else if (result?.user?.idToken?.tokenString != null) {
                continuation.resume(result.user.idToken!!.tokenString)
            } else {
                continuation.resumeWithException(Exception("Failed to get ID token"))
            }
        }
    }
}
```

### Step 5: Update CocoaPods Configuration

Update `shared/build.gradle.kts` cocoapods section to include GoogleSignIn:

```kotlin
cocoapods {
    // ... existing config ...
    
    pod("GoogleSignIn") {
        version = "7.0.0"
    }
}
```

Then rebuild the framework:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### Step 6: Test on iOS

1. Run the iOS app from Xcode
2. On the Auth screen, tap **Continue with Google**
3. Select your Google account
4. You should be signed in and navigated to the Home screen
5. Tap the logout icon to test logout

## Troubleshooting

### Android

**Error: "Developer Error" or "Sign-In Failed"**
- ❗ **Most common issue:** Missing or incorrect SHA-1 fingerprint
  - Make sure you added your debug SHA-1 to Firebase
  - Verify it matches the keystore you're using
- Make sure you've added the correct Web Client ID in code
- Download a fresh `google-services.json` after adding SHA-1 and enabling Google Sign-In
- Clean and rebuild: `./gradlew clean build`

**Error: "API not enabled"**
- Go to Google Cloud Console
- Enable the **Google Identity Toolkit API**

**Error: "Sign-In was cancelled"**
- Check that the google-services.json is up to date
- Verify the package name matches in Firebase and your app

**How to verify your SHA-1 is correct:**
```bash
# Get SHA-1 from your debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Compare with what's in Firebase Console
# They should match exactly
```

### iOS

**Error: "No such module 'GoogleSignIn'"**
- Make sure you ran `pod install` in the iosApp directory
- Open the `.xcworkspace` file, not `.xcodeproj`

**App crashes on sign-in**
- Verify the URL scheme is correctly configured
- Check that the reversed client ID matches your GoogleService-Info.plist

## Testing Email/Password Sign-In

The app also supports email/password authentication:

1. On the Auth screen, enter an email and password
2. Tap **Sign Up** to create a new account
3. Tap **Sign In** to sign in with an existing account
4. Use the logout button to sign out

## Current Features

✅ Email/Password Sign-In
✅ Email/Password Sign-Up
✅ Google Sign-In (Android - requires configuration)
✅ Google Sign-In (iOS - requires additional setup)
✅ Logout Button in Home Screen
✅ User Display Name/Email in Top Bar
✅ Auto-navigation on Sign-In/Sign-Out

## Next Steps

After setting up Google Sign-In, you can:
- Test both authentication methods
- Verify user data is displayed correctly
- Test logout functionality
- Start building the puzzle game features

