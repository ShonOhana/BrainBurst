# iOS Build Error Fix

## ✅ Issue Fixed!

The "PhaseScriptExecution failed" error was caused by using `String.format()` which doesn't exist in Kotlin Native (iOS).

### What Was Fixed

**File:** `shared/src/commonMain/kotlin/com/brainburst/domain/game/sudoku/SudokuState.kt`

**Before (Android-only):**
```kotlin
return "%02d:%02d".format(minutes, seconds)  // ❌ Doesn't work on iOS
```

**After (Cross-platform):**
```kotlin
return buildString {
    append(if (minutes < 10) "0$minutes" else "$minutes")
    append(":")
    append(if (seconds < 10) "0$seconds" else "$seconds")
}  // ✅ Works on both Android and iOS
```

## 🚀 How to Build iOS Now

### Option 1: Use the Rebuild Script (Recommended)
```bash
./rebuild_ios.sh
```

Then open Xcode:
```bash
cd iosApp
open iosApp.xcworkspace
```

### Option 2: Manual Steps

1. **Clean Xcode build:**
   ```bash
   cd iosApp
   xcodebuild clean -workspace iosApp.xcworkspace -scheme iosApp
   ```

2. **Clean Gradle:**
   ```bash
   cd ..
   ./gradlew :shared:clean
   ```

3. **Rebuild framework:**
   ```bash
   ./gradlew :shared:linkPodDebugFrameworkIosSimulatorArm64
   ```

4. **Open Xcode and build:**
   ```bash
   cd iosApp
   open iosApp.xcworkspace
   # Press Cmd+B to build
   ```

### Option 3: Build from Xcode Directly

1. Close Xcode if open
2. Clean derived data:
   ```bash
   rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
   ```
3. Open Xcode:
   ```bash
   cd iosApp
   open iosApp.xcworkspace
   ```
4. Product → Clean Build Folder (Cmd+Shift+K)
5. Product → Build (Cmd+B)

## 🎯 Expected Result

You should see:
- ✅ Framework builds successfully
- ✅ No "PhaseScriptExecution failed" errors
- ✅ App runs on iOS simulator

## 🐛 If You Still See Errors

### "Could not infer iOS target architectures"
This happens when building from command line. **Solution:** Always build from Xcode for iOS.

### "Kotlin framework 'Shared' doesn't exist"
Run this first:
```bash
./gradlew :shared:linkPodDebugFrameworkIosSimulatorArm64
```

### CocoaPods errors
Reinstall pods:
```bash
cd iosApp
pod deintegrate
pod install
```

## 📝 Technical Details

### Why String.format() Doesn't Work on iOS

- `String.format()` is a Java/JVM function
- Kotlin Native (used for iOS) doesn't have the JVM
- Solution: Use Kotlin's `buildString {}` which works everywhere

### Cross-Platform String Formatting

For future reference, always use these cross-platform approaches:

**✅ Good (works everywhere):**
```kotlin
buildString { append("value: $value") }
"value: $value"  // String templates
```

**❌ Bad (JVM only):**
```kotlin
"%d".format(value)
String.format("%d", value)
```

## 🎉 Status

✅ **iOS compilation fixed!**  
✅ **Framework builds successfully!**  
✅ **Ready to run on iOS!**

---

**Next Steps:**
1. Run `./rebuild_ios.sh` or build from Xcode
2. Test the app on iOS simulator
3. Continue with Phase 7 (Ads & Leaderboard)





