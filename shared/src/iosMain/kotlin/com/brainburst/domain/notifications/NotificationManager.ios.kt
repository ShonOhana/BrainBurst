package com.brainburst.domain.notifications

/**
 * iOS implementation of NotificationManager
 * TODO: Implement iOS-specific notifications when needed
 */
actual class NotificationManager {
    actual suspend fun scheduleDailyNotifications() {
        // TODO: Implement iOS notifications
    }
    
    actual suspend fun cancelDailyNotifications() {
        // TODO: Implement iOS notifications
    }
    
    actual suspend fun hasNotificationPermission(): Boolean {
        // TODO: Implement iOS permission check
        return false
    }
    
    actual suspend fun requestNotificationPermission() {
        // TODO: Implement iOS permission request
    }
}
