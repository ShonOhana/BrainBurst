package com.brainburst.domain.notifications

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

actual class NotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "daily_puzzle_channel"
        const val CHANNEL_NAME = "Daily Puzzle Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for new daily puzzles"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "daily_puzzle_notification"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    actual suspend fun scheduleDailyNotifications() {
        // Schedule daily work at 9 AM (when new puzzles are generated)
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(
            calculateInitialDelay(), TimeUnit.MILLISECONDS
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,  // REPLACE so new schedules override old ones
            dailyWorkRequest
        )
    }
    
    actual suspend fun cancelDailyNotifications() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    actual suspend fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed before Android 13
        }
    }
    
    actual suspend fun requestNotificationPermission() {
        // On Android 13+, we need to open app settings for permission
        // The permission dialog needs to be triggered from an Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = android.content.Intent().apply {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to general app settings
                try {
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Show a notification immediately (used by the worker)
     */
    suspend fun showNotification(title: String, message: String) {
        if (!hasNotificationPermission()) {
            return
        }
        
        // Create intent to open the app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info) // Using system icon as fallback
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
    
    private fun calculateInitialDelay(): Long {
        val now = System.currentTimeMillis()
        
        // Use UTC timezone to match backend puzzle generation (9 AM UTC)
        val utcTimeZone = java.util.TimeZone.getTimeZone("UTC")
        val calendar = java.util.Calendar.getInstance(utcTimeZone).apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 9)  // 9 AM UTC
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            
            // If 9 AM UTC has already passed today, schedule for tomorrow
            if (timeInMillis <= now) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return calendar.timeInMillis - now
    }
}
