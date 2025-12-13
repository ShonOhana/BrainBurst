# BrainBurst: Daily Puzzles

A Compose Multiplatform app for daily puzzle challenges across Android and iOS.

## Project Structure

```
BrainBurst/
├── shared/               # Shared Kotlin Multiplatform module
│   ├── commonMain/      # Common code for all platforms
│   ├── androidMain/     # Android-specific code
│   └── iosMain/         # iOS-specific code
├── androidApp/          # Android application module
└── iosApp/             # iOS application (Xcode project)
```

## Tech Stack

- **Kotlin 2.2.21**: Latest Kotlin with Xcode 26.1 support
- **Kotlin Multiplatform**: Share code between Android and iOS
- **Compose Multiplatform**: Unified UI framework
- **Firebase (GitLive)**: Authentication and Firestore
- **Material 3**: Modern design system
- **Koin**: Dependency injection
- **Kotlin Coroutines**: Async operations

## Requirements

### Android
- Android Studio (latest)
- JDK 17+

### iOS
- macOS with Xcode 26.1+ (or Xcode 16.x+)
- CocoaPods installed: `sudo gem install cocoapods`

## Getting Started

### Android

1. Open project in Android Studio
2. Sync Gradle files
3. Run the `androidApp` configuration

### iOS

**First Time Setup:**

1. Add CocoaPods path to `local.properties`:
   ```
   kotlin.apple.cocoapods.bin=/opt/homebrew/bin/pod
   ```
   (Or find your path with `which pod`)

2. Install CocoaPods dependencies:
   ```bash
   cd iosApp
   pod install
   ```

3. **Important**: Open `iosApp/iosApp.xcworkspace` (NOT `.xcodeproj`) in Xcode

4. Build the Shared framework:
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```

5. Run the app from Xcode (`Cmd + R`)

## Development Phases

- [x] Phase 1: Project & KMP Setup
- [x] Phase 2: Firebase Integration
- [ ] Phase 3: Domain & Game Infrastructure
- [ ] Phase 4: Auth & Navigation
- [ ] Phase 5: Home Screen
- [ ] Phase 6: Sudoku UI & Logic
- [ ] Phase 7: Ads & Leaderboard
- [ ] Phase 8: Backend GPT + Firestore

## Current Status

✅ Compose Multiplatform project structure created
✅ Material 3 theme configured
✅ Basic "Hello BrainBurst" UI working
✅ Android and iOS entry points wired
✅ Firebase Auth and Firestore integrated
✅ Koin dependency injection configured
✅ iOS app with CocoaPods working on Xcode 26.1

## Next Steps

- Build domain models for games
- Implement authentication flow UI
- Create home screen with game cards
- Build Sudoku 6x6 game engine and UI

---

For detailed specifications, see [SPEC.md](SPEC.md)


