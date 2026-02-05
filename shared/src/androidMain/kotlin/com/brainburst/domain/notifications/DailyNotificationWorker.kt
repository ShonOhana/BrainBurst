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
            println("DailyNotificationWorker: Starting work at ${System.currentTimeMillis()}")
            
            // Check if notifications are enabled
            val notificationsEnabled = preferencesRepository.getNotificationsEnabled().first()
            println("DailyNotificationWorker: Notifications enabled = $notificationsEnabled")
            
            if (!notificationsEnabled) {
                println("DailyNotificationWorker: Notifications disabled, skipping")
                return Result.success()
            }
            
            // Check if user is logged in
            val currentUser = authRepository.currentUser.value
            println("DailyNotificationWorker: Current user = ${currentUser?.uid}")
            
            if (currentUser == null) {
                // User not logged in, don't send notification
                println("DailyNotificationWorker: No user logged in, skipping")
                return Result.success()
            }
            
            // Check if user has completed all today's puzzles
            val hasUnsolvedPuzzle = GameType.entries.any { gameType ->
                val hasCompleted = puzzleRepository.hasUserCompletedToday(
                    userId = currentUser.uid,
                    gameType = gameType
                ).getOrNull() ?: false
                
                println("DailyNotificationWorker: $gameType completed = $hasCompleted")
                
                // Return true if this puzzle is NOT completed (i.e., unsolved)
                !hasCompleted
            }
            
            println("DailyNotificationWorker: Has unsolved puzzle = $hasUnsolvedPuzzle")
            
            // Only send notification if there's at least one unsolved puzzle
            if (!hasUnsolvedPuzzle) {
                // User has solved all puzzles, don't send notification
                println("DailyNotificationWorker: All puzzles solved, skipping notification")
                return Result.success()
            }
            
            // Show the notification
            println("DailyNotificationWorker: Showing notification")
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "New Daily Puzzle! 🧩",
                message = "Today's brain teaser is ready. Start solving now!"
            )
            
            println("DailyNotificationWorker: Work completed successfully")
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            println("DailyNotificationWorker: Error occurred - ${e.message}")
            return Result.retry()
        }
    }
}
