package com.brainburst.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSOperationQueue

/**
 * iOS implementation: Observes app-level lifecycle notifications.
 */
@Composable
actual fun PlatformLifecycleHandler(
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    DisposableEffect(Unit) {
        val notificationCenter = NSNotificationCenter.defaultCenter
        
        // Notification name constants (NSNotificationName is a typealias for String)
        val didEnterBackgroundNotification = "UIApplicationDidEnterBackgroundNotification"
        val willEnterForegroundNotification = "UIApplicationWillEnterForegroundNotification"
        
        // Observer for app going to background
        val didEnterBackgroundObserver = notificationCenter.addObserverForName(
            name = didEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _: NSNotification? ->
            onPause()
        }
        
        // Observer for app coming to foreground
        val willEnterForegroundObserver = notificationCenter.addObserverForName(
            name = willEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _: NSNotification? ->
            onResume()
        }
        
        onDispose {
            notificationCenter.removeObserver(didEnterBackgroundObserver)
            notificationCenter.removeObserver(willEnterForegroundObserver)
        }
    }
}

