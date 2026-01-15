# Firebase Crashlytics Setup

Firebase Crashlytics has been successfully integrated into the BrainBurst project for both Android and iOS platforms.

## What Was Done

### Android
1. **Gradle Configuration**
   - Added Crashlytics plugin version (`3.0.2`) to `gradle/libs.versions.toml`
   - Added Crashlytics dependency version (`19.2.1`) to `gradle/libs.versions.toml`
   - Applied the Crashlytics plugin in both `build.gradle.kts` files
   - Added the Crashlytics dependency to `androidApp/build.gradle.kts`

2. **Initialization**
   - Crashlytics is automatically initialized when Firebase initializes via the google-services plugin
   - No additional code changes needed in `BrainBurstApplication.kt`

### iOS
1. **CocoaPods Configuration**
   - Added `FirebaseCrashlytics` pod (version `11.5.0`) to `Podfile`
   - Imported `FirebaseCrashlytics` in `iOSApp.swift`

2. **Initialization**
   - Crashlytics is automatically initialized when `FirebaseApp.configure()` is called
   - No additional initialization code required

## Next Steps

### 1. Install Dependencies

**For Android:**
```bash
./gradlew clean build
```

**For iOS:**
```bash
cd iosApp
pod install
```

### 2. Build Your App

**Important:** You must build and run your app at least once for Crashlytics to register with the Firebase console.

**Android:**
```bash
./gradlew androidApp:assembleDebug
```

**iOS:**
Open `iosApp/iosApp.xcworkspace` in Xcode and build the project.

### 3. Enable Crashlytics in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your BrainBurst project
3. Navigate to **Crashlytics** in the left sidebar
4. Click **Enable Crashlytics** (if not already enabled)
5. After the first build, you should see "Waiting for data" - this is normal
6. Once you run the app, it will appear in the console

## Testing Crashlytics

### Force a Test Crash

**Android:**
Add this code to test crash reporting:
```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

// Force a crash for testing
throw RuntimeException("Test Crash for Crashlytics")

// Or log a non-fatal exception
FirebaseCrashlytics.getInstance().recordException(Exception("Test Exception"))
```

**iOS:**
Add this code to test crash reporting:
```swift
import FirebaseCrashlytics

// Force a crash for testing
fatalError("Test Crash for Crashlytics")

// Or log a non-fatal exception
Crashlytics.crashlytics().record(error: NSError(domain: "TestDomain", code: 100))
```

### Custom Logging

**Android:**
```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

val crashlytics = FirebaseCrashlytics.getInstance()

// Set user identifier
crashlytics.setUserId("user123")

// Add custom keys
crashlytics.setCustomKey("level", "5")
crashlytics.setCustomKey("puzzle_type", "sudoku")

// Log custom messages
crashlytics.log("User completed puzzle")

// Log non-fatal exceptions
try {
    // Your code
} catch (e: Exception) {
    crashlytics.recordException(e)
}
```

**iOS:**
```swift
import FirebaseCrashlytics

let crashlytics = Crashlytics.crashlytics()

// Set user identifier
crashlytics.setUserID("user123")

// Add custom keys
crashlytics.setCustomValue(5, forKey: "level")
crashlytics.setCustomValue("sudoku", forKey: "puzzle_type")

// Log custom messages
crashlytics.log("User completed puzzle")

// Log non-fatal exceptions
do {
    // Your code
} catch {
    crashlytics.record(error: error)
}
```

## Viewing Crash Reports

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your BrainBurst project
3. Click **Crashlytics** in the left menu
4. View crashes, errors, and analytics

### Report Details Include:
- Stack traces
- Device information
- OS version
- App version
- Custom keys and logs
- Number of affected users
- Frequency of occurrence

## Build Types

### Debug Builds
- Crashlytics is enabled but reports are not uploaded by default
- To test in debug, add to `androidApp/build.gradle.kts`:
```kotlin
buildTypes {
    debug {
        firebaseCrashlytics {
            mappingFileUploadEnabled = false
        }
    }
}
```

### Release Builds
- Crashlytics automatically uploads mapping files for deobfuscation
- ProGuard/R8 mapping files are automatically uploaded
- Full crash reporting is enabled

## Automatic Features

Crashlytics automatically collects:
- ✅ Fatal crashes
- ✅ ANRs (Application Not Responding) on Android
- ✅ Memory warnings on iOS
- ✅ App version
- ✅ Device model and OS version
- ✅ Free disk space and memory
- ✅ Orientation
- ✅ Network connection type

## Privacy Considerations

Crashlytics collects:
- Crash stack traces
- Device identifiers
- Performance data
- Custom logs you add

Make sure to:
- Update your Privacy Policy to mention crash reporting
- Inform users about data collection
- Consider GDPR compliance if applicable

## Troubleshooting

### Android: "Missing google-services.json"
- Ensure `google-services.json` is in `androidApp/` directory
- Sync Gradle after making changes

### iOS: "Missing GoogleService-Info.plist"
- Ensure `GoogleService-Info.plist` is in `iosApp/iosApp/` directory
- Rebuild the project

### "No crashes appear in console"
- Wait up to 5 minutes for first-time registration
- Ensure you've run the app at least once
- Check that Firebase project ID matches your configuration
- Verify internet connection on device/emulator

### iOS Build Script (Optional)
If crashes don't upload automatically, add a build phase script in Xcode:
1. Open `iosApp.xcworkspace` in Xcode
2. Select your target → Build Phases
3. Click "+" → New Run Script Phase
4. Add:
```bash
"${PODS_ROOT}/FirebaseCrashlytics/run"
```

## Additional Resources

- [Firebase Crashlytics Documentation](https://firebase.google.com/docs/crashlytics)
- [Android Crashlytics Guide](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [iOS Crashlytics Guide](https://firebase.google.com/docs/crashlytics/get-started?platform=ios)

## Status

✅ Crashlytics configured for Android
✅ Crashlytics configured for iOS
⏳ Awaiting first build and run to complete registration

Run the app on both platforms to complete the setup!
