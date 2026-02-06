package com.brainburst.domain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainburst.domain.model.GameType
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.PreferencesRepository
import com.brainburst.domain.repository.PuzzleRepository
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
    private val authRepository: AuthRepository by inject()
    private val puzzleRepository: PuzzleRepository by inject()
    
    override suspend fun doWork(): Result {
        try {
            println("DailyNotificationWorker: ============================================")
            println("DailyNotificationWorker: WORKER STARTED at ${System.currentTimeMillis()}")
            println("DailyNotificationWorker: ============================================")
            
            // Check if notifications are enabled
            val notificationsEnabled = preferencesRepository.getNotificationsEnabled().first()
            println("DailyNotificationWorker: Notifications enabled = $notificationsEnabled")
            
            if (!notificationsEnabled) {
                println("DailyNotificationWorker: Notifications disabled, skipping")
                return Result.success()
            }
            
            // Show the notification (removed user login check - notifications work regardless)
            println("DailyNotificationWorker: Showing notification")
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "Your Daily Puzzle is Here! 🧩",
                message = "Solve today before they're gone. You in?"
            )
            
            println("DailyNotificationWorker: ============================================")
            println("DailyNotificationWorker: Notification sent successfully!")
            println("DailyNotificationWorker: ============================================")
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            println("DailyNotificationWorker: ============================================")
            println("DailyNotificationWorker: ERROR OCCURRED!")
            println("DailyNotificationWorker: Error = ${e.message}")
            println("DailyNotificationWorker: Stack trace:")
            e.printStackTrace()
            println("DailyNotificationWorker: ============================================")
            return Result.retry()
        }
    }
}
