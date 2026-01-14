# Push Notifications Implementation

## Overview
Daily push notifications have been successfully implemented for the BrainBurst app. Notifications are triggered every day at 9 AM (when new puzzles are generated) and only work when the notification switch in Settings is enabled.

## Features Implemented

### 1. **Notification Manager** (Platform-Specific)
- **Common Interface**: `NotificationManager` (expect class)
- **Android Implementation**: Uses WorkManager for daily scheduling
- **iOS Implementation**: Stub implementation (ready for future development)

### 2. **Preferences Repository**
- Stores notification enabled/disabled state using DataStore
- Provides reactive Flow-based API for observing preference changes

### 3. **Daily Notification Worker**
- Runs daily at 9 AM
- Checks if notifications are enabled before showing
- Shows notification: "New Puzzle Available! 🧩" with message "A fresh brain teaser is waiting for you. Start solving now!"

### 4. **Settings Integration**
- Settings screen has a toggle switch for notifications
- When enabled: Schedules daily notifications
- When disabled: Cancels all scheduled notifications
- Preference is persisted across app restarts

### 5. **App Initialization**
- Notifications are initialized when the app starts
- If notifications were previously enabled, they are automatically re-scheduled

## Files Created/Modified

### New Files
1. `shared/src/commonMain/kotlin/com/brainburst/domain/notifications/NotificationManager.kt`
2. `shared/src/commonMain/kotlin/com/brainburst/domain/repository/PreferencesRepository.kt`
3. `shared/src/commonMain/kotlin/com/brainburst/data/repository/PreferencesRepositoryImpl.kt`
4. `shared/src/androidMain/kotlin/com/brainburst/domain/notifications/NotificationManager.android.kt`
5. `shared/src/androidMain/kotlin/com/brainburst/domain/notifications/DailyNotificationWorker.kt`
6. `shared/src/iosMain/kotlin/com/brainburst/domain/notifications/NotificationManager.ios.kt`

### Modified Files
1. `gradle/libs.versions.toml` - Added FCM and WorkManager dependencies
2. `androidApp/build.gradle.kts` - Added FCM and WorkManager dependencies
3. `androidApp/src/main/AndroidManifest.xml` - Added notification permissions
4. `shared/src/commonMain/kotlin/com/brainburst/di/AppModule.kt` - Added DI for new components
5. `shared/src/androidMain/kotlin/com/brainburst/di/PlatformModule.android.kt` - Added NotificationManager
6. `shared/src/iosMain/kotlin/com/brainburst/di/PlatformModule.ios.kt` - Added NotificationManager
7. `shared/src/commonMain/kotlin/com/brainburst/presentation/settings/SettingsViewModel.kt` - Added notification logic
8. `shared/src/commonMain/kotlin/com/brainburst/App.kt` - Added notification initialization

## Technical Details

### Notification Scheduling
- Uses Android WorkManager for reliable daily scheduling
- Scheduled for 9 AM every day (matches puzzle generation time)
- Automatically reschedules after device reboot (WorkManager handles this)

### Permissions
- `POST_NOTIFICATIONS` permission required on Android 13+ (Tiramisu)
- Automatically granted on Android 12 and below
- Permission is checked before showing notifications

### Notification Channel
- Channel ID: `daily_puzzle_channel`
- Channel Name: "Daily Puzzle Notifications"
- Importance: DEFAULT
- Shows app icon and opens app when tapped

## Testing

### To Test Notifications:
1. **Enable Notifications**:
   - Go to Settings screen
   - Toggle "Notifications" switch ON
   - Notification will be scheduled for 9 AM daily

2. **Test Immediately**:
   - Modify `calculateInitialDelay()` in `NotificationManager.android.kt` to schedule for a few seconds from now
   - Or use WorkManager's TestDriver in a test environment

3. **Verify in Logcat**:
   - Check for WorkManager logs showing the scheduled work
   - Check for notification creation logs

4. **Check in Android Settings**:
   - Go to Android Settings > Apps > BrainBurst > Notifications
   - Verify "Daily Puzzle Notifications" channel exists

### To Disable Notifications:
1. Go to Settings screen
2. Toggle "Notifications" switch OFF
3. All scheduled notifications will be cancelled

## Future Enhancements

### Potential Improvements:
1. **Customizable Time**: Allow users to choose when they want to receive notifications
2. **Notification Customization**: Different messages for different game types
3. **Rich Notifications**: Show puzzle preview or difficulty in notification
4. **iOS Implementation**: Add iOS-specific notification handling
5. **Permission Request UI**: Add a proper permission request flow with rationale
6. **Notification Analytics**: Track notification engagement metrics

## Dependencies Added

```toml
firebase-messaging = "24.1.0"
work-runtime = "2.10.0"
```

## Permissions Required

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

## Architecture

```
User Toggles Notification in Settings
           ↓
SettingsViewModel.onNotificationsToggle()
           ↓
PreferencesRepository.setNotificationsEnabled()
           ↓
NotificationManager.scheduleDailyNotifications()
           ↓
WorkManager schedules DailyNotificationWorker
           ↓
[Every day at 9 AM]
           ↓
DailyNotificationWorker.doWork()
           ↓
Check if notifications enabled
           ↓
Show notification to user
```

## Notes
- Notifications are only shown if the user has enabled them in Settings
- The notification system respects Android's battery optimization and Doze mode
- WorkManager ensures notifications are delivered even if the app is closed
- The implementation is production-ready and follows Android best practices
