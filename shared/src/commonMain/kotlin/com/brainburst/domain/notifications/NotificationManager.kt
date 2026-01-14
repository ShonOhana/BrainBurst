package com.brainburst.domain.notifications

/**
 * Platform-specific notification manager
 */
expect class NotificationManager {
    /**
     * Schedule daily notifications for new puzzles
     */
    suspend fun scheduleDailyNotifications()
    
    /**
     * Cancel all scheduled notifications
     */
    suspend fun cancelDailyNotifications()
    
    /**
     * Check if notification permission is granted
     */
    suspend fun hasNotificationPermission(): Boolean
    
    /**
     * Request notification permission from the user
     */
    suspend fun requestNotificationPermission()
}
