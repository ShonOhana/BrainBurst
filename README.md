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

- **Kotlin Multiplatform**: Share code between Android and iOS
- **Compose Multiplatform**: Unified UI framework
- **Firebase (GitLive)**: Authentication and Firestore
- **Material 3**: Modern design system
- **Koin**: Dependency injection
- **Kotlin Coroutines**: Async operations

## Getting Started

### Android

1. Open project in Android Studio
2. Sync Gradle files
3. Run the `androidApp` configuration

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Build the Shared framework first from Android Studio: `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
3. Run the app from Xcode

## Development Phases

- [x] Phase 1: Project & KMP Setup
- [ ] Phase 2: Firebase Integration
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

## Next Steps

- Integrate Firebase Auth and Firestore
- Build domain models for games
- Implement authentication flow
- Create home screen with game cards
- Build Sudoku 6x6 game engine and UI

---

For detailed specifications, see [SPEC.md](SPEC.md)

