package com.brainburst.domain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainburst.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that runs daily to show notification for new puzzles
 */
class DailyNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    
    private val preferencesRepository: PreferencesRepository by inject()
    
    override suspend fun doWork(): Result {
        try {
            // Check if notifications are enabled
            val notificationsEnabled = preferencesRepository.getNotificationsEnabled().first()
            
            if (!notificationsEnabled) {
                return Result.success()
            }
            
            // Show the notification
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "New Daily Puzzle! 🧩",
                message = "Today's brain teaser is ready. Start solving now!"
            )
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
