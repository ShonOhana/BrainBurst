# Notification Testing Guide

## Quick Start

### 1. Build and Run the App
```bash
./gradlew :androidApp:assembleDebug
```

### 2. Test Notification Toggle
1. Open the BrainBurst app
2. Navigate to Settings (gear icon)
3. Toggle the "Notifications" switch **ON**
4. The notification is now scheduled for 9 AM daily

### 3. Verify Scheduling Works
The notification is scheduled for 9 AM every day. To test it immediately:

#### Option A: Wait until 9 AM
- Simply wait until 9 AM the next day
- You should receive a notification: "New Puzzle Available! 🧩"

#### Option B: Test Immediately (Modify Code)
1. Open `NotificationManager.android.kt`
2. Find the `calculateInitialDelay()` function
3. Replace it with:
```kotlin
private fun calculateInitialDelay(): Long {
    return 10_000L // 10 seconds
}
```
4. Rebuild and run the app
5. Toggle notifications ON in Settings
6. Wait 10 seconds - you should see the notification!

#### Option C: Use ADB to Trigger WorkManager (Advanced)
```bash
# Force WorkManager to run the scheduled task
adb shell cmd jobscheduler run -f com.brainburst.android 0
```

### 4. Verify Notification Appears
When the notification appears:
- ✅ Title: "New Puzzle Available! 🧩"
- ✅ Message: "A fresh brain teaser is waiting for you. Start solving now!"
- ✅ Tapping it opens the BrainBurst app
- ✅ App icon is shown

### 5. Test Disabling Notifications
1. Go back to Settings
2. Toggle the "Notifications" switch **OFF**
3. All scheduled notifications are now cancelled
4. No notifications will be shown at 9 AM

## Verify Persistence

### Test 1: App Restart
1. Enable notifications in Settings
2. Close the app completely (swipe away from recent apps)
3. Reopen the app
4. Notification should still be scheduled (WorkManager persists across restarts)

### Test 2: Device Reboot
1. Enable notifications in Settings
2. Reboot your device
3. WorkManager will automatically reschedule the notification
4. Notification should still appear at 9 AM

## Check Android Settings

### Verify Notification Channel
1. Open Android Settings
2. Go to Apps → BrainBurst → Notifications
3. You should see "Daily Puzzle Notifications" channel
4. Make sure it's enabled

### Grant Permissions (Android 13+)
If you're on Android 13 or higher:
1. First time enabling notifications in the app
2. Android will prompt you to grant notification permission
3. Tap "Allow" to receive notifications

## Debugging

### Check Logcat for Logs
```bash
adb logcat | grep -i "notification\|workmanager"
```

Look for:
- WorkManager scheduling logs
- Notification creation logs
- Any error messages

### Check WorkManager Status
You can check scheduled work in Android Studio:
1. Open "App Inspection" → "Background Task Inspector"
2. Look for "daily_puzzle_notification" work

### Check DataStore Preferences
The notification preference is stored in DataStore:
```bash
# Navigate to app data
adb shell
cd /data/data/com.brainburst.android/files/datastore/
cat brainburst_preferences.preferences_pb
```

## Common Issues

### Issue: Notification doesn't appear at 9 AM
**Solutions:**
1. Check if notifications are enabled in Android Settings
2. Verify battery optimization isn't restricting the app
3. Check if "Do Not Disturb" mode is active
4. Verify the device clock is correct

### Issue: Permission dialog doesn't appear (Android 13+)
**Solutions:**
1. Uninstall and reinstall the app
2. Clear app data in Android Settings
3. Check if permission was previously denied (go to App Settings → Permissions)

### Issue: Notification appears immediately on every app open
**Solution:**
- This is expected behavior due to the LaunchedEffect in App.kt
- The notification is scheduled once, but WorkManager prevents duplicate scheduling
- If you see multiple notifications, check for bugs in the scheduling logic

### Issue: Toggle doesn't persist
**Solutions:**
1. Check DataStore is properly initialized
2. Verify Koin dependencies are correct
3. Check logcat for any exceptions

## Expected Behavior Summary

| Action | Expected Result |
|--------|----------------|
| Toggle ON | Notification scheduled for 9 AM daily |
| Toggle OFF | All notifications cancelled |
| App restart | Notification schedule persists |
| Device reboot | Notification schedule persists |
| 9 AM arrives | Notification appears if toggle is ON |
| Tap notification | Opens BrainBurst app |

## Production Checklist

Before releasing to production, verify:
- [ ] Notifications appear reliably at 9 AM
- [ ] Toggle persists across app restarts
- [ ] Toggle persists across device reboots
- [ ] Notification opens the app correctly
- [ ] Notification channel is properly named and configured
- [ ] Permission request flow works on Android 13+
- [ ] Battery optimization doesn't prevent notifications
- [ ] Notifications respect user's Do Not Disturb settings
- [ ] Multiple users can have different notification preferences

## Customization

### Change Notification Time
Edit `NotificationManager.android.kt`:
```kotlin
set(java.util.Calendar.HOUR_OF_DAY, 9)  // Change to desired hour (24-hour format)
set(java.util.Calendar.MINUTE, 0)       // Change to desired minute
```

### Change Notification Content
Edit `DailyNotificationWorker.kt`:
```kotlin
notificationManager.showNotification(
    title = "Your custom title",
    message = "Your custom message"
)
```

### Change Notification Icon
Replace `R.mipmap.brain_burst` in `NotificationManager.android.kt`:
```kotlin
.setSmallIcon(R.drawable.your_notification_icon)
```

## Next Steps

1. Test notifications thoroughly on different Android versions
2. Test on different devices (Samsung, Pixel, etc.)
3. Monitor notification delivery rate in production
4. Consider adding notification analytics
5. Consider adding customizable notification time in settings
6. Implement iOS notifications using similar architecture
